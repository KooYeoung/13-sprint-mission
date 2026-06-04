package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@ConditionalOnProperty(
      prefix = "discodeit.repository",
      name = "type",
      havingValue = "file"
)
public class FileBinaryContentRepository implements BinaryContentRepository {
   private final FileObjectStorage<BinaryContent> storage =
         new FileObjectStorage<>("data/binary-content");

   @Override
   public BinaryContent save(BinaryContent binaryContent) {
      storage.save(binaryContent);
      return binaryContent;
   }

   @Override
   public Optional<BinaryContent> findById(UUID id) {

      return Optional.ofNullable(storage.load(id));
   }

   @Override
   public List<BinaryContent> findAllByIdIn(List<UUID> ids) {

      return storage.loadAll().stream()
            .filter(bc -> ids.contains(bc.getId()))
            .toList();
   }

   @Override
   public void delete(UUID id) {
      storage.delete(id);
   }
}
