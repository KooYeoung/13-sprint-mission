package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.config.JpaAuditingTestConfig;
import com.sprint.mission.discodeit.config.P6SpySqlFormatter;
import com.sprint.mission.discodeit.config.QuerydslTestConfig;
import com.sprint.mission.discodeit.entity.BinaryContent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest(showSql = false)
@Import(value = {QuerydslTestConfig.class, JpaAuditingTestConfig.class, P6SpySqlFormatter.class})
@DisplayName("BinaryContentRepository 슬라이스 테스트")
class BinaryContentRepositoryTest {

    @Autowired
    BinaryContentRepository binaryContentRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("파일 목록 조회 성공 - ID 목록에 포함된 파일만 반환")
    void findAllByIdIn_returnsBinaryContentsMatchingIds() {
        // given
        // 이 테스트의 대상은 BinaryContentRepository.findAllByIdIn(...) derived query다.
        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고, 실제 BinaryContent 엔티티를
        // H2 테스트 DB에 저장한 뒤 repository 메서드가 id in (...) 조건으로 올바르게 조회하는지 검증한다.
        //
        // 저장한 모든 파일 ID를 그대로 조회하면 "ID 목록에 포함된 파일만 반환"한다는 조건이 약하게 검증된다.
        // 그래서 조회 대상 파일 3개와 제외 대상 파일 1개를 함께 저장한 뒤,
        // 조회 대상 파일 ID만 findAllByIdIn(...)에 전달한다.
        List<BinaryContent> binaryContentsToFind = List.of(
                createBinaryContent("avatar.png", "image/png", 1_000L),
                createBinaryContent("profile.jpg", "image/jpeg", 2_000L),
                createBinaryContent("manual.pdf", "application/pdf", 3_000L)
        );
        BinaryContent binaryContentToExclude = createBinaryContent("excluded.txt", "text/plain", 4_000L);

        // saveAllAndFlush(...)로 insert SQL을 즉시 DB에 반영한다.
        // saveAll(...)만 사용하면 flush 시점이 테스트 흐름 밖으로 미뤄질 수 있어,
        // Repository 쿼리가 실제 DB row를 대상으로 동작한다는 의도가 덜 분명해진다.
        List<BinaryContent> savedBinaryContents = binaryContentRepository.saveAllAndFlush(binaryContentsToFind);
        BinaryContent savedBinaryContentToExclude = binaryContentRepository.saveAndFlush(binaryContentToExclude);

        // 조회 조건으로 사용할 ID 목록은 저장된 조회 대상 파일에서만 만든다.
        // 제외 대상 파일의 ID는 DB에 존재하지만 이 목록에는 포함하지 않는다.
        List<UUID> binaryContentIds = savedBinaryContents.stream()
                .map(BinaryContent::getId)
                .toList();
        UUID excludedBinaryContentId = savedBinaryContentToExclude.getId();

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 반환될 수 없다.
        // 즉, 아래 when 절의 결과는 1차 캐시가 아니라 실제 SELECT 결과라는 점이 분명해진다.
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // 조회 대상 ID 목록은 비어 있지 않아야 하고, 제외 대상 파일도 실제 DB 저장 대상이어야 한다.
        // 또한 제외 대상 ID가 조회 조건 목록에 들어가 있으면 이 테스트가 "제외"를 검증할 수 없다.
        assertThat(binaryContentIds).isNotEmpty();
        assertThat(excludedBinaryContentId)
                .isNotNull()
                .isNotIn(binaryContentIds);

        // when
        // 조회 대상 파일 ID 목록에 포함된 BinaryContent만 조회한다.
        List<BinaryContent> foundBinaryContents = binaryContentRepository.findAllByIdIn(binaryContentIds);

        // then
        // 조회 결과는 요청한 ID 개수와 같아야 한다.
        // Spring Data JPA의 in 쿼리는 별도 order by가 없으면 반환 순서를 보장하지 않으므로,
        // 순서가 아니라 ID 집합이 같은지를 검증한다.
        assertThat(foundBinaryContents).isNotEmpty();
        assertThat(foundBinaryContents).hasSize(binaryContentIds.size());

        assertThat(foundBinaryContents.stream().map(BinaryContent::getId))
                .containsExactlyInAnyOrderElementsOf(binaryContentIds);
        assertThat(foundBinaryContents.stream().map(BinaryContent::getId))
                .doesNotContain(excludedBinaryContentId);

        // ID뿐 아니라 주요 컬럼 값도 함께 확인한다.
        // 이렇게 하면 다른 row가 섞여 들어오거나, 조회 결과가 요청한 파일과 다른 값으로 매핑되는 문제를 잡을 수 있다.
        assertThat(foundBinaryContents)
                .extracting(
                        BinaryContent::getId,
                        BinaryContent::getOriginalFileName,
                        BinaryContent::getContentType,
                        BinaryContent::getSize
                )
                .containsExactlyInAnyOrder(
                        tuple(savedBinaryContents.get(0).getId(), "avatar.png", "image/png", 1_000L),
                        tuple(savedBinaryContents.get(1).getId(), "profile.jpg", "image/jpeg", 2_000L),
                        tuple(savedBinaryContents.get(2).getId(), "manual.pdf", "application/pdf", 3_000L)
                );
    }

    @Test
    @DisplayName("파일 목록 조회 성공 - 일치하는 ID가 없으면 빈 목록 반환")
    void findAllByIdIn_returnsEmptyList_whenIdsDoNotExist() {
        // given
        // 이 테스트의 대상은 BinaryContentRepository.findAllByIdIn(...) derived query의 negative case다.
        // 검증하려는 조건은 "binary_contents 테이블에 파일 row는 존재하지만,
        // 조회 ID 목록과 일치하는 row가 하나도 없으면 빈 목록을 반환한다"는 것이다.
        //
        // 빈 테이블에서 없는 ID를 조회하면 empty가 나오는 것이 당연하므로 테스트 의미가 약하다.
        // 그래서 실제 BinaryContent 여러 개를 먼저 저장한 뒤,
        // 저장된 ID와 전혀 겹치지 않는 ID 목록으로 조회한다.
        List<BinaryContent> savedTargetCandidates = List.of(
                createBinaryContent("avatar.png", "image/png", 1_000L),
                createBinaryContent("profile.jpg", "image/jpeg", 2_000L),
                createBinaryContent("manual.pdf", "application/pdf", 3_000L)
        );
        BinaryContent savedExtraCandidate = createBinaryContent("excluded.txt", "text/plain", 4_000L);

        // saveAllAndFlush(...)와 saveAndFlush(...)로 insert SQL을 즉시 DB에 반영한다.
        // 이후 조회가 영속성 컨텍스트의 객체가 아니라 실제 DB row를 기준으로 동작한다는 의도를 명확히 한다.
        List<BinaryContent> savedBinaryContents = binaryContentRepository.saveAllAndFlush(savedTargetCandidates);
        BinaryContent savedBinaryContentToExclude = binaryContentRepository.saveAndFlush(savedExtraCandidate);

        List<UUID> savedBinaryContentIds = savedBinaryContents.stream()
                .map(BinaryContent::getId)
                .toList();
        UUID savedExtraBinaryContentId = savedBinaryContentToExclude.getId();

        // 조회 조건으로 사용할 ID들은 저장하지 않은 값이다.
        // UUID.randomUUID()를 사용하되, 아래 사전 조건에서 실제 저장된 ID들과 겹치지 않는지 확인한다.
        List<UUID> missingBinaryContentIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        em.clear();

        // 사전 조건을 먼저 확인한다.
        // DB에는 실제 파일 row가 있어야 하고, 조회할 ID 목록은 저장된 어떤 파일 ID와도 겹치면 안 된다.
        assertThat(savedBinaryContentIds).isNotEmpty();
        assertThat(savedExtraBinaryContentId).isNotNull();
        assertThat(missingBinaryContentIds)
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .doesNotContain(savedExtraBinaryContentId);
        assertThat(savedBinaryContentIds).doesNotContainAnyElementsOf(missingBinaryContentIds);

        // when
        // DB에 존재하지 않는 ID 목록으로 파일 목록을 조회한다.
        List<BinaryContent> foundBinaryContents = binaryContentRepository.findAllByIdIn(missingBinaryContentIds);

        // then
        // binary_contents 테이블에 row가 있더라도, id in (...) 조건에 일치하는 row가 없으면 빈 목록이 반환되어야 한다.
        assertThat(foundBinaryContents).isEmpty();
    }

    @Test
    @DisplayName("파일 목록 삭제 성공 - ID 목록에 포함된 파일만 삭제")
    void deleteAllByIdIn_deletesBinaryContentsMatchingIds() {
        // given
        // 이 테스트의 대상은 BinaryContentRepository.deleteAllByIdIn(...) derived delete query다.
        // 검증하려는 조건은 "전달한 ID 목록에 포함된 파일만 삭제하고,
        // 목록에 포함되지 않은 파일은 그대로 남긴다"는 것이다.
        //
        // 삭제 대상 파일 3개와 삭제 제외 파일 1개를 함께 저장한다.
        // 삭제 후 대상 ID들은 더 이상 조회되지 않아야 하고, 제외 대상 파일은 여전히 조회되어야 한다.
        List<BinaryContent> binaryContentsToDelete = List.of(
                createBinaryContent("avatar.png", "image/png", 1_000L),
                createBinaryContent("profile.jpg", "image/jpeg", 2_000L),
                createBinaryContent("manual.pdf", "application/pdf", 3_000L)
        );
        BinaryContent binaryContentToExclude = createBinaryContent("excluded.txt", "text/plain", 4_000L);

        // saveAllAndFlush(...)로 삭제 대상 파일들을 먼저 DB에 반영한다.
        // 제외 대상도 별도로 flush해 두어, 삭제 전후에 실제 DB row로 비교할 수 있게 한다.
        List<BinaryContent> savedBinaryContents = binaryContentRepository.saveAllAndFlush(binaryContentsToDelete);
        BinaryContent savedBinaryContentToExclude = binaryContentRepository.saveAndFlush(binaryContentToExclude);

        // 삭제 조건으로 사용할 ID 목록은 삭제 대상 파일에서만 만든다.
        // 제외 대상 파일의 ID는 DB에 존재하지만 삭제 조건 목록에는 포함하지 않는다.
        List<UUID> binaryContentIds = savedBinaryContents.stream()
                .map(BinaryContent::getId)
                .toList();
        UUID excludedBinaryContentId = savedBinaryContentToExclude.getId();

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 조회 결과에 영향을 주지 않는다.
        // 삭제 전 확인, 삭제 후 재조회 모두 실제 DB 상태를 기준으로 검증하기 위한 정리다.
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // 삭제 대상 ID 목록은 비어 있지 않아야 하고, 제외 대상 ID는 삭제 대상 목록에 포함되면 안 된다.
        assertThat(binaryContentIds).isNotEmpty();
        assertThat(excludedBinaryContentId)
                .isNotNull()
                .isNotIn(binaryContentIds);

        // 삭제 전에는 삭제 대상 파일과 제외 대상 파일이 모두 DB에서 조회되어야 한다.
        // 이 확인이 있어야 삭제 후 empty가 "처음부터 없어서"가 아니라 delete 결과임을 분명히 할 수 있다.
        List<BinaryContent> foundBinaryContentsBeforeDelete = binaryContentRepository.findAllByIdIn(binaryContentIds);
        List<BinaryContent> foundExcludedBinaryContentsBeforeDelete = binaryContentRepository.findAllByIdIn(List.of(excludedBinaryContentId));

        assertThat(foundBinaryContentsBeforeDelete)
                .isNotEmpty()
                .hasSize(binaryContentIds.size());
        assertThat(foundExcludedBinaryContentsBeforeDelete)
                .hasSize(1)
                .extracting(
                        BinaryContent::getId,
                        BinaryContent::getOriginalFileName,
                        BinaryContent::getContentType,
                        BinaryContent::getSize
                )
                .containsExactly(tuple(excludedBinaryContentId, "excluded.txt", "text/plain", 4_000L));

        // when
        // ID 목록에 포함된 BinaryContent만 삭제한다.
        binaryContentRepository.deleteAllByIdIn(binaryContentIds);

        // deleteAllByIdIn(...)의 삭제 SQL을 DB에 즉시 반영하고,
        // 남아 있을 수 있는 영속성 컨텍스트 상태를 비워 삭제 후 재조회 결과가 실제 DB 기준이 되게 한다.
        em.flush();
        em.clear();

        // then
        // 삭제 대상 ID로 다시 조회하면 아무 row도 반환되지 않아야 한다.
        List<BinaryContent> deletedBinaryContents = binaryContentRepository.findAllByIdIn(binaryContentIds);
        assertThat(deletedBinaryContents).isEmpty();

        // 반대로 삭제 조건에 포함하지 않은 파일은 그대로 남아 있어야 한다.
        // 이 검증이 있어야 deleteAllByIdIn(...)가 전체 삭제가 아니라 ID 목록 기반 부분 삭제임을 확인할 수 있다.
        List<BinaryContent> remainingBinaryContents = binaryContentRepository.findAllByIdIn(List.of(excludedBinaryContentId));
        assertThat(remainingBinaryContents)
                .hasSize(1)
                .extracting(
                        BinaryContent::getId,
                        BinaryContent::getOriginalFileName,
                        BinaryContent::getContentType,
                        BinaryContent::getSize
                )
                .containsExactly(tuple(excludedBinaryContentId, "excluded.txt", "text/plain", 4_000L));
    }

    private BinaryContent createBinaryContent(String originalFileName, String contentType, Long size) {
        return new BinaryContent(originalFileName, contentType, size);
    }
}
