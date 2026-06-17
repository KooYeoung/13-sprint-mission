package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
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
public class JCFUserRepository implements UserRepository {

   private final Map<UUID, User> userMap = new HashMap<>();

   @Override
   public void save(User user) {
      userMap.put(user.getId(), user);
   }

   @Override
   public Optional<User> findById(UUID userId) {
      return Optional.ofNullable(userMap.get(userId));
   }

   @Override
   public List<User> findAll() {
      return userMap.values().stream().toList();
   }

   @Override
   public void delete(UUID userId) {
      userMap.remove(userId);
   }
}
