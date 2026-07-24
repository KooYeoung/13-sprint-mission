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
    @DisplayName("P6Spy SQL 포맷 성공 - 템플릿 SQL이 없으면 실행 SQL을 출력하지 않음")
    void formatMessage_returnsEmpty_whenPreparedSqlIsBlank() {
        // given
        // prepared가 비어 있는데 sql만 출력하면, literal 값이 포함된 SQL이 그대로 로그에 남을 수 있다.
        String sql = "select * from users where email = 'test@example.com'";

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
        // 안전한 템플릿 SQL이 없으면 로그를 남기지 않는다.
        assertThat(message).isEmpty();
    }

}