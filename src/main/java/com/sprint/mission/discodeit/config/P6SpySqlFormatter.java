package com.sprint.mission.discodeit.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class P6SpySqlFormatter implements MessageFormattingStrategy {

    private static final String LINE_SEPARATOR = System.lineSeparator();

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance()
                .setLogMessageFormat(P6SpySqlFormatter.class.getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (prepared == null || prepared.isBlank()) {
            if (Category.BATCH.getName().equals(category)) {
                if (isSafeTemplateSql(sql)) {
                    return formatLogMessage(category, elapsed, connectionId, formatSql(category, sql).strip());
                }

                return formatLogMessage(
                        category,
                        elapsed,
                        connectionId,
                        "JDBC batch executed, but P6Spy did not provide a safe SQL template."
                );
            }
            return "";
        }

        // P6Spy의 sql 인자는 바인딩 값이 치환된 실행 SQL일 수 있으므로 statement 로그에는 prepared 템플릿만 사용한다.
        String formattedSql = formatSql(category, prepared).strip();
        return formatLogMessage(category, elapsed, connectionId, formattedSql);
    }

    private boolean isSafeTemplateSql(String sql) {
        return sql != null
                && !sql.isBlank()
                && sql.contains("?");
    }

    private String formatLogMessage(String category, long elapsed, int connectionId, String message) {
        return String.format(
                "%s [%s] | %d ms | connection=%d %s %s",
                LINE_SEPARATOR,
                category,
                elapsed,
                connectionId,
                LINE_SEPARATOR,
                message
        );
    }

    private String formatSql(String category, String sql) {
        if (!Category.STATEMENT.getName().equals(category)
                && !Category.BATCH.getName().equals(category)) {
            return sql;
        }

        String normalizedSql = sql
                .strip()
                .toLowerCase(Locale.ROOT);

        if (isDdl(normalizedSql)) {
            return FormatStyle.DDL
                    .getFormatter()
                    .format(sql);
        }

        return FormatStyle.BASIC
                .getFormatter()
                .format(sql);
    }

    private boolean isDdl(String sql) {
        return sql.startsWith("create")
                || sql.startsWith("alter")
                || sql.startsWith("drop")
                || sql.startsWith("truncate")
                || sql.startsWith("comment");
    }

}
