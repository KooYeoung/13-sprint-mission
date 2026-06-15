package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
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
public class JCFReadStatusRepository implements ReadStatusRepository {

   private final Map<UUID, ReadStatus> readStatusMap = new HashMap<>();

   @Override
   public ReadStatus save(ReadStatus readStatus) {
      readStatusMap.put(readStatus.getId(), readStatus);
      return readStatus;
   }

   @Override
   public Optional<ReadStatus> findById(UUID id) {
      return Optional.ofNullable(readStatusMap.get(id));
   }

   @Override
   public List<ReadStatus> findAll() {
      return new ArrayList<>(readStatusMap.values());
   }

   @Override
   public List<ReadStatus> findByUserId(UUID userId) {
      return readStatusMap.values()
            .stream()
            .filter(r -> r.getUserId().equals(userId))
            .toList();
   }

   @Override
   public List<ReadStatus> findByChannelId(UUID channelId) {
      return readStatusMap.values()
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
      readStatusMap.remove(id);
   }

   @Override
   public void deleteByChannelId(UUID channelId) {
      findByChannelId(channelId)
            .stream()
            .map(ReadStatus::getId)
            .forEach(this::delete);
   }
}
