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
    @DisplayName("P6Spy SQL 포맷 성공 - 템플릿 SQL이 없으면 실행 SQL을 fallback으로 출력")
    void formatMessage_usesSqlFallback_whenPreparedSqlIsBlank() {
        // given
        // P6Spy 이벤트 중에는 prepared SQL 템플릿이 비어 있고 sql 인자만 채워지는 경우가 있다.
        // INSERT/DELETE 같은 DML 로그를 확인하려면 이 경우 sql 인자를 fallback으로 출력해야 한다.
        //
        // 단, sql 인자에는 바인딩 값이 치환되어 들어올 수 있으므로 운영 로그에서는 주의해야 한다.
        String sql = "delete from read_statuses where id=?";

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
        assertThat(message).contains("[statement] | 3 ms | connection=1");
        assertThat(message).contains("delete");
        assertThat(message).contains("read_statuses");
        assertThat(message).contains("?");
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
    @DisplayName("P6Spy SQL 포맷 성공 - 배치 이벤트는 템플릿 SQL이 없어도 실행 SQL 출력")
    void formatMessage_logsBatchSql_whenPreparedSqlIsBlank() {
        // given
        // JDBC batch 이벤트에서는 P6Spy가 prepared SQL 템플릿을 비워서 넘기는 경우가 있다.
        // 이때 기존처럼 빈 문자열을 반환하면 로그에는 "p6spy -"만 남아,
        // batch가 실행됐는지조차 확인하기 어렵다.
        //
        // batch SQL까지 보고 싶은 테스트/개발 환경에서는 sql 인자를 fallback으로 출력한다.
        // 단, sql 인자에는 바인딩 값이 치환되어 들어올 수 있으므로 운영 로그에 그대로 쓰면 안 된다.
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
