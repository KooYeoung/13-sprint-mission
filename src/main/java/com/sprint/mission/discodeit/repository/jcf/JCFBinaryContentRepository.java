package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
@ConditionalOnProperty(
      prefix = "discodeit.repository",
      name = "type",
      havingValue = "jcf",
      matchIfMissing = true
)
public class JCFBinaryContentRepository implements BinaryContentRepository {
   private final Map<UUID, BinaryContent> binaryContentMap = new HashMap<>();

   @Override
   public BinaryContent save(BinaryContent binaryContent) {
      binaryContentMap.put(binaryContent.getId(), binaryContent);
      return binaryContent;
   }

   @Override
   public Optional<BinaryContent> findById(UUID id) {
      return Optional.ofNullable(binaryContentMap.get(id));
   }

   @Override
   public List<BinaryContent> findAllByIdIn(List<UUID> ids) {
      return binaryContentMap.values().stream()
            .filter(bc -> ids.contains(bc.getId()))
            .toList();
   }

   @Override
   public void delete(UUID id) {
      binaryContentMap.remove(id);
   }
}
