package com.sprint.mission.discodeit.repository.jfc;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JCFUserRepository implements UserRepository {

   private final HashMap<UUID, User> userHashMap= new HashMap<>();

   @Override
   public void save(User user) {
      userHashMap.put(user.getId(), user);
   }

   @Override
   public User findById(UUID userId) {
      return userHashMap.get(userId);
   }

   @Override
   public List<User> findAll() {
      return userHashMap.values().stream().toList();
   }

   @Override
   public void delete(UUID userId) {
      userHashMap.remove(userId);
   }
}
