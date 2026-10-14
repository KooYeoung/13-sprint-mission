/*
 * 저장소 루트에서 실행한다. 서버는 매 측정마다 재시작한다(H2와 업로드 경로 초기화).
 * 터미널 1 — 동기 서버:
 *   bash gradlew bootTestRun --args='--discodeit.async.enabled=false'
 * 터미널 2 — 동기 측정:
 *   mkdir -p build/reports/performance/k6
 *   k6 run -e MODE=sync --summary-export=build/reports/performance/k6/sync.json scripts/k6/message-create.js
 * 동기 서버를 Ctrl+C로 종료한 후 터미널 1 — 비동기 서버:
 *   bash gradlew bootTestRun --args='--discodeit.async.enabled=true'
 * 터미널 2 — 비동기 측정:
 *   k6 run -e MODE=async --summary-export=build/reports/performance/k6/async.json scripts/k6/message-create.js
 *
 * MODE는 결과 식별용이다. 서버의 비동기 설정은 위 실행 인자로 변경한다.
 * Java 17 / 최대 힙 512MiB / H2 / 로컬 저장 지연 3초 / 동일 풀 설정.
 * VU 1명, 워밍업 3회, 측정 20회. 요청마다 파일·알림 완료 후 다음 요청을 보낸다.
 * 비교 지표는 message_create_duration, completion_duration, actuator_mean_ms이다.
 * 전체 http_req_*에는 로그인·워밍업·완료 확인도 포함되므로 비교에 사용하지 않는다.
 * 파일 생성 전 다운로드 재시도의 타임아웃도 http_req_failed에 포함된다.
 * 메시지 생성·최종 완료 성공 여부는 message_create_failures와 completion_failures로 확인한다.
 * 완료 시간은 API로 관찰한 시간이며 DB의 SUCCESS 저장 여부는 JUnit에서 검증한다.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';
import crypto from 'k6/crypto';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:18080';
const MODE = __ENV.MODE;
if (!['sync', 'async'].includes(MODE)) throw new Error('MODE=sync 또는 MODE=async를 지정하세요.');
const WARMUPS = 3;
const SAMPLES = 20;
const CONTENT = 'identical performance message';
const CHANNEL = 'performance';
const AUTHOR = 'k6-author';
const payload = new Uint8Array(1024 * 1024);
for (let i = 0; i < payload.length; i++) payload[i] = i % 251;
const payloadHash = crypto.sha256(payload.buffer, 'hex');
const createDuration = new Trend('message_create_duration', true);
const completionDuration = new Trend('completion_duration', true);
const samples = new Counter('message_create_samples');
const createFailures = new Rate('message_create_failures');
const completionFailures = new Rate('completion_failures');
const actuatorCount = new Gauge('actuator_count');
const actuatorTotal = new Gauge('actuator_total_seconds');
const actuatorMean = new Gauge('actuator_mean_ms');
let beforeMetrics;

export const options = {
    scenarios: { comparison: { executor: 'per-vu-iterations', vus: 1, iterations: WARMUPS + SAMPLES, maxDuration: '3m' } },
    tags: { mode: MODE },
    summaryTrendStats: ['avg', 'med', 'p(95)', 'max'],
    thresholds: {
        checks: ['rate==1'],
        message_create_samples: [`count==${SAMPLES}`],
        message_create_failures: ['rate==0'],
        completion_failures: ['rate==0'],
        actuator_count: [`value==${SAMPLES}`],
    },
};

function requireResult(condition, message) {
    if (!check(condition, { [message]: (value) => value })) throw new Error(message);
}

function request(method, path, session = {}, body = null, phase = 'setup', extra = {}) {
    const headers = {};
    if (session.token) headers.Authorization = `Bearer ${session.token}`;
    if (session.csrf) headers['X-XSRF-TOKEN'] = session.csrf;
    return http.request(method, BASE_URL + path, body, {
        timeout: '15s',
        // 사용자별 쿠키가 섞이지 않도록 매 요청에 해당 사용자의 CSRF만 전달한다.
        jar: new http.CookieJar(),
        cookies: session.csrf ? { 'XSRF-TOKEN': session.csrf } : {},
        tags: { phase, name: path.replace(/[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}/gi, ':id') },
        ...extra,
        headers: { ...headers, ...extra.headers },
    });
}

function jsonResult(response, status) {
    requireResult(response.status === status, `HTTP ${status} 응답`);
    return response.json();
}

function jsonRequest(method, path, session, body) {
    return request(method, path, session, JSON.stringify(body), 'setup', { headers: { 'Content-Type': 'application/json' } });
}

function refreshCsrf(session) {
    const response = request('GET', '/api/auth/csrf-token', session);
    requireResult(response.status === 204, 'CSRF 발급 성공');
    session.csrf = response.cookies['XSRF-TOKEN']?.[0]?.value;
    requireResult(Boolean(session.csrf), 'CSRF 쿠키 존재');
}

function login(username, password) {
    const session = {};
    refreshCsrf(session);
    const result = jsonResult(request('POST', '/api/auth/login', session, { username, password }), 200);
    session.token = result.accessToken;
    requireResult(Boolean(session.token), '액세스 토큰 존재');
    delete session.csrf;
    refreshCsrf(session);
    return session;
}

function createUser(admin, username) {
    const password = 'k6-password123!';
    const user = jsonResult(request('POST', '/api/users', admin, {
        userCreateRequest: http.file(JSON.stringify({ username, password, email: `${username}@performance.local` }), 'user.json', 'application/json'),
    }), 200);
    return { id: user.id, ...login(username, password) };
}

export function setup() {
    const admin = login('test-admin', 'test-admin-password');
    const author = createUser(admin, AUTHOR);
    const receiver = createUser(admin, 'k6-receiver');
    const channel = jsonResult(jsonRequest('POST', '/api/channels/public', admin, { name: CHANNEL, description: 'k6 comparison' }), 201);
    for (const user of [author, receiver]) {
        const read = jsonResult(jsonRequest('POST', '/api/readStatuses', user, {
            userId: user.id, channelId: channel.id, lastReadAt: '1970-01-01T00:00:00Z',
        }), 201);
        const updated = jsonResult(jsonRequest('PATCH', `/api/readStatuses/${read.id}`, user, {
            newLastReadAt: '1970-01-01T00:00:00Z', newNotificationEnabled: true,
        }), 200);
        requireResult(updated.notificationEnabled === true, '채널 알림 활성화');
    }
    return { admin, author, receiver, channelId: channel.id };
}

function metrics(admin) {
    const response = jsonResult(request('GET', '/actuator/metrics/message.create.async', admin, null, 'actuator'), 200);
    const count = response.measurements.find((metric) => metric.statistic === 'COUNT')?.value;
    const total = response.measurements.find((metric) => metric.statistic === 'TOTAL_TIME')?.value;
    requireResult(Number.isFinite(count) && Number.isFinite(total), 'Actuator 계측값 존재');
    return { count, total };
}

function awaitCompletion(data, fileId, expectedCount) {
    const deadline = Date.now() + 15000;
    let fileReady = false;
    while (Date.now() < deadline) {
        if (!fileReady) {
            const response = request('GET', `/api/binaryContents/${fileId}/download`, data.author, null, 'completion', {
                responseType: 'binary', timeout: `${Math.max(1, Math.min(500, deadline - Date.now()))}ms`,
                // 파일 생성 전에는 본문이 오지 않을 수도 있어 개별 조회를 짧게 제한한다.
                responseCallback: http.expectedStatuses(200, 404, 500),
            });
            requireResult(![401, 403].includes(response.status), '다운로드 인증·인가 성공');
            fileReady = response.status === 200 && response.body?.byteLength === payload.length
                && crypto.sha256(response.body, 'hex') === payloadHash;
        }
        const notifications = jsonResult(request('GET', '/api/notifications', data.receiver, null, 'completion', {
            timeout: `${Math.max(1, deadline - Date.now())}ms`,
        }), 200);
        requireResult(notifications.length <= expectedCount, '알림 중복 생성 없음');
        if (fileReady && notifications.length === expectedCount) {
            requireResult(notifications.every((notification) => notification.receiverId === data.receiver.id
                && notification.title === `${AUTHOR} (#${CHANNEL})` && notification.content === CONTENT), '수신자 알림 내용 일치');
            const sent = jsonResult(request('GET', '/api/notifications', data.author, null, 'completion', {
                timeout: `${Math.max(1, deadline - Date.now())}ms`,
            }), 200);
            requireResult(sent.length === 0, '작성자 알림 제외');
            requireResult(Date.now() <= deadline, '15초 내 업로드·알림 완료');
            return;
        }
        sleep(0.1);
    }
    throw new Error('파일 내용과 수신자 알림을 15초 안에 확인하지 못했습니다.');
}

export default function (data) {
    const iteration = exec.scenario.iterationInTest;
    const measured = iteration >= WARMUPS;
    try {
        if (iteration === WARMUPS) {
            beforeMetrics = metrics(data.admin);
            requireResult(beforeMetrics.count === WARMUPS, '워밍업 3건 계측 및 초기 상태 확인');
        }
        const body = {
            messageCreateRequest: http.file(JSON.stringify({ content: CONTENT, authorId: data.author.id, channelId: data.channelId }), 'message.json', 'application/json'),
            attachments: http.file(payload.buffer, 'comparison.bin', 'application/octet-stream'),
        };
        const started = Date.now();
        const response = request('POST', '/api/messages', data.author, body, measured ? 'measurement' : 'warmup');
        if (measured) {
            samples.add(1);
            createDuration.add(response.timings.duration);
            createFailures.add(response.status !== 201);
        }
        const message = jsonResult(response, 201);
        requireResult(Boolean(message.id) && message.content === CONTENT && message.channelId === data.channelId
            && message.author.id === data.author.id && message.attachments.length === 1
            && message.attachments[0].size === payload.length, '메시지와 첨부 파일 응답 일치');
        awaitCompletion(data, message.attachments[0].id, iteration + 1);
        if (measured) {
            completionDuration.add(Date.now() - started);
            completionFailures.add(false);
        }
        if (iteration === WARMUPS + SAMPLES - 1) {
            const after = metrics(data.admin);
            const count = after.count - beforeMetrics.count;
            const total = after.total - beforeMetrics.total;
            actuatorCount.add(count);
            actuatorTotal.add(total);
            requireResult(count === SAMPLES && total > 0, '본 측정 20건 Actuator 집계');
            actuatorMean.add(total * 1000 / count);
        }
    } catch (error) {
        if (measured) completionFailures.add(true);
        exec.test.abort(`성능 비교 실패: ${error.message}`);
    }
}
