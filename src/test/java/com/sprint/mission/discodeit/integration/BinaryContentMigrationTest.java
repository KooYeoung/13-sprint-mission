package com.sprint.mission.discodeit.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("바이너리 파일 업로드 상태 PostgreSQL 마이그레이션 테스트")
class BinaryContentMigrationTest {

    @Test
    @DisplayName("V4는 기존 파일을 완료 상태로 보정하고 상태 제약과 변경 시각을 추가한다")
    void migrateV4_backfillsExistingMetadataAndRemovesTemporaryDefault() {
        // Docker가 없을 때 성공 또는 건너뜀으로 처리하지 않고 검증 불가를 실패로 드러낸다.
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")) {
            postgres.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            Flyway.configure().dataSource(dataSource).target("3").load().migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            UUID existingId = UUID.randomUUID();
            jdbc.update("INSERT INTO binary_contents (id, original_file_name, size, content_type) VALUES (?, ?, ?, ?)",
                    existingId, "existing.txt", 10L, "text/plain");

            Flyway flyway = Flyway.configure().dataSource(dataSource).target("4").load();
            flyway.migrate();
            flyway.validate();

            assertThat(jdbc.queryForObject("SELECT status FROM binary_contents WHERE id = ?", String.class, existingId))
                    .isEqualTo("SUCCESS");
            assertThat(jdbc.queryForObject("""
                    SELECT data_type FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'binary_contents' AND column_name = 'updated_at'
                    """, String.class)).isEqualTo("timestamp with time zone");
            assertThat(jdbc.queryForObject("""
                    SELECT is_nullable FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'binary_contents' AND column_name = 'status'
                    """, String.class)).isEqualTo("NO");
            assertThat(jdbc.queryForObject("""
                    SELECT column_default FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'binary_contents' AND column_name = 'status'
                    """, String.class)).isNull();
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO binary_contents (original_file_name, size, content_type)
                    VALUES ('without-status.txt', 1, 'text/plain')
                    """)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            UUID newId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO binary_contents (id, original_file_name, size, content_type, status)
                    VALUES (?, 'new.txt', 1, 'text/plain', 'PROCESSING')
                    """, newId);
            assertThat(jdbc.queryForObject("SELECT status FROM binary_contents WHERE id = ?", String.class, newId))
                    .isEqualTo("PROCESSING");
        }
    }
}
