package com.sprint.mission.discodeit.config;

import com.p6spy.engine.logging.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class P6SpySqlFormatterTest {

    private final P6SpySqlFormatter formatter = new P6SpySqlFormatter();

    @Test
    @DisplayName("P6Spy SQL 포맷 성공 - 바인딩 값이 치환된 SQL 대신 템플릿 SQL 출력")
    void formatMessage_usesPreparedSql_whenSqlContainsBoundValues() {
        // given
        // P6Spy의 prepared 인자는 바인딩 전 SQL 템플릿이고, sql 인자는 바인딩 값이 치환된 실행 SQL일 수 있다.
        // 이메일, 비밀번호 같은 민감 값이 로그에 남지 않도록 formatter는 prepared만 사용해야 한다.
        String prepared = "select * from users where email = ? and password = ?";
        String sql = "select * from users where email = 'test@example.com' and password = 'secret-password'";

        // when
        String message = formatter.formatMessage(
                1,
                "",
                3L,
                Category.STATEMENT.getName(),
                prepared,
                sql,
                ""
        );

        // then
        // 로그에는 실행 시간, connection id, SQL 템플릿 정보만 남긴다.
        assertThat(message).contains("[statement] | 3 ms | connection=1");
        assertThat(message).contains("users");
        assertThat(message).contains("?");

        // 바인딩 값이 치환된 SQL을 사용하면 아래 민감 값이 그대로 노출된다.
        // 이 값들이 포함되지 않는지 확인해서 로그 노출 회귀를 막는다.
        assertThat(message)
                .doesNotContain("test@example.com")
                .doesNotContain("secret-password");
    }

    @Test
    @DisplayName("P6Spy SQL 포맷 성공 - statement는 템플릿 SQL이 없으면 로그를 출력하지 않음")
    void formatMessage_returnsEmptyStatementLog_whenPreparedSqlIsBlank() {
        // given
        // P6Spy 이벤트 중에는 prepared SQL 템플릿이 비어 있고 sql 인자만 채워지는 경우가 있다.
        // 하지만 sql 인자는 바인딩 값이 치환된 실행 SQL일 수 있으므로 그대로 출력하면 민감 값이 노출될 수 있다.
        //
        // statement 카테고리에서는 안전한 prepared 템플릿이 없으면 sql fallback을 사용하지 않는다.
        // 이 테스트는 prepared가 비어 있을 때 실행 SQL 출력이 다시 도입되는 회귀를 막는다.
        String sql = "select * from users where email = 'test@example.com' and password = 'secret-password'";

        // when
        String message = formatter.formatMessage(
                1,
                "",
                3L,
                Category.STATEMENT.getName(),
                "",
                sql,
                ""
        );

        // then
        // 안전한 템플릿 SQL이 없으므로 로그를 남기지 않는다.
        // 따라서 sql 인자에 들어온 이메일이나 비밀번호도 노출되지 않는다.
        assertThat(message).isEmpty();
        assertThat(message)
                .doesNotContain("test@example.com")
                .doesNotContain("secret-password");
    }

    @Test
    @DisplayName("P6Spy SQL 포맷 성공 - 템플릿 SQL과 실행 SQL이 모두 없으면 로그를 출력하지 않음")
    void formatMessage_returnsEmpty_whenPreparedSqlAndSqlAreBlank() {
        // when
        String message = formatter.formatMessage(
                1,
                "",
                3L,
                Category.STATEMENT.getName(),
                "",
                "",
                ""
        );

        // then
        assertThat(message).isEmpty();
    }

    @Test
    @DisplayName("P6Spy SQL 포맷 성공 - 배치 이벤트는 안전한 템플릿 SQL이면 제한적으로 출력")
    void formatMessage_logsBatchTemplateSql_whenPreparedSqlIsBlank() {
        // given
        // JDBC batch 이벤트에서는 P6Spy가 prepared SQL 템플릿을 비워서 넘기는 경우가 있다.
        // 이때 기존처럼 빈 문자열을 반환하면 로그에는 "p6spy -"만 남아,
        // batch가 실행됐는지조차 확인하기 어렵다.
        //
        // 다만 sql 인자는 바인딩 값이 치환된 실행 SQL일 수 있다.
        // 그래서 batch 진단용 fallback은 placeholder가 남아 있는 템플릿 형태일 때만 허용한다.
        String sql = "insert into read_statuses (channel_id, user_id, id) values (?, ?, ?)";

        // when
        String message = formatter.formatMessage(
                1,
                "",
                3L,
                Category.BATCH.getName(),
                "",
                sql,
                ""
        );

        // then
        // prepared가 비어 있어도 batch 카테고리와 SQL 본문이 함께 남아야 한다.
        assertThat(message).contains("[batch] | 3 ms | connection=1");
        assertThat(message).contains("insert");
        assertThat(message).contains("read_statuses");
        assertThat(message).contains("?");
    }

    @Test
    @DisplayName("P6Spy SQL 포맷 성공 - 배치 이벤트도 실행 SQL이면 안전 메시지만 출력")
    void formatMessage_logsSafeBatchEvent_whenBatchSqlContainsBoundValues() {
        // given
        // batch 카테고리라도 sql 인자에 실제 UUID, 이메일, 비밀번호 같은 값이 치환되어 들어올 수 있다.
        // prepared 템플릿이 없고 sql도 placeholder를 포함하지 않으면 안전한 SQL 템플릿이라고 볼 수 없다.
        //
        // 이 경우 SQL 본문을 출력하지 않고 batch 실행 사실만 남겨 민감 값 노출을 막는다.
        String sql = "insert into users (email, password, id) "
                + "values ('test@example.com', 'secret-password', '11111111-1111-1111-1111-111111111111')";

        // when
        String message = formatter.formatMessage(
                1,
                "",
                3L,
                Category.BATCH.getName(),
                "",
                sql,
                ""
        );

        // then
        assertThat(message).contains("[batch] | 3 ms | connection=1");
        assertThat(message).contains("JDBC batch executed");
        assertThat(message)
                .doesNotContain("test@example.com")
                .doesNotContain("secret-password")
                .doesNotContain("11111111-1111-1111-1111-111111111111");
    }

    @Test
    @DisplayName("P6Spy SQL formatting - batch SQL with question mark literal is not logged")
    void formatMessage_logsSafeBatchEvent_whenBatchSqlContainsQuestionMarkInsideLiteral() {
        // given
        String sql = "insert into users (email, id) "
                + "values ('a?b@example.com', '11111111-1111-1111-1111-111111111111')";

        // when
        String message = formatter.formatMessage(
                1,
                "",
                3L,
                Category.BATCH.getName(),
                "",
                sql,
                ""
        );

        // then
        assertThat(message).contains("[batch] | 3 ms | connection=1");
        assertThat(message).contains("JDBC batch executed");
        assertThat(message)
                .doesNotContain("a?b@example.com")
                .doesNotContain("11111111-1111-1111-1111-111111111111");
    }

    @Test
    @DisplayName("P6Spy SQL 포맷 성공 - 배치 이벤트도 SQL이 없으면 실행 여부만 출력")
    void formatMessage_logsBatchEvent_whenPreparedSqlAndSqlAreBlank() {
        // given
        // P6Spy batch 이벤트에 prepared와 sql이 모두 비어 들어오면 출력할 SQL 본문이 없다.
        // 그래도 빈 로그만 남기면 원인을 알기 어려우므로 batch 실행 사실은 표시한다.

        // when
        String message = formatter.formatMessage(
                1,
                "",
                3L,
                Category.BATCH.getName(),
                "",
                "",
                ""
        );

        // then
        assertThat(message).contains("[batch] | 3 ms | connection=1");
        assertThat(message).contains("JDBC batch executed");
    }

}
