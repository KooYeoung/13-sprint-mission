package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.config.RepositoryProperties;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(
      prefix = "discodeit.repository",
      name = "type",
      havingValue = "file"
)
public class FileReadStatusRepository implements ReadStatusRepository {
   private final FileObjectStorage<ReadStatus> storage;

   public FileReadStatusRepository(RepositoryProperties properties) {
      this. storage = new FileObjectStorage<>(
              properties.getFileDirectory().resolve("read-status")
      );
   }

   @Override
   public ReadStatus save(ReadStatus readStatus) {
      storage.save(readStatus);
      return readStatus;
   }

   @Override
   public Optional<ReadStatus> findById(UUID id) {
      return Optional.ofNullable(storage.load(id));
   }

   @Override
   public List<ReadStatus> findAll() {
      return storage.loadAll();
   }

   @Override
   public List<ReadStatus> findByUserId(UUID userId) {
      return storage.loadAll()
            .stream()
            .filter(r -> r.getUserId().equals(userId))
            .toList();
   }

   @Override
   public List<ReadStatus> findByChannelId(UUID channelId) {
      return storage.loadAll()
            .stream()
            .filter(r -> r.getChannelId().equals(channelId))
            .toList();
   }

   @Override
   public ReadStatus update(ReadStatus readStatus) {
      return save(readStatus);
   }

   @Override
   public void delete(UUID id) {
      storage.delete(id);
   }

   @Override
   public void deleteByChannelId(UUID channelId) {
      findByChannelId(channelId)
            .stream()
            .map(ReadStatus::getId)
            .forEach(this::delete);
   }
}
