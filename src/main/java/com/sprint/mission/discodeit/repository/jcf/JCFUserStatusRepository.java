package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
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
public class JCFUserStatusRepository implements UserStatusRepository {
   private final Map<UUID, UserStatus> userStatusMap = new HashMap<>();

   @Override
   public UserStatus save(UserStatus userStatus) {
      userStatusMap.put(userStatus.getId(), userStatus);
      return userStatus;

   }

   @Override
   public Optional<UserStatus> findById(UUID id) {
      return Optional.ofNullable(userStatusMap.get(id));
   }

   @Override
   public Optional<UserStatus> findByUserId(UUID userId) {

      return userStatusMap.values().stream()
            .filter(us -> us.getUserId().equals(userId))
            .findFirst();
   }

   @Override
   public List<UserStatus> findAll() {
      return new ArrayList<>(userStatusMap.values());
   }

   @Override
   public UserStatus update(UserStatus userStatus) {
      return save(userStatus);
   }

   @Override
   public void delete(UUID id) {
      userStatusMap.remove(id);
   }
}
