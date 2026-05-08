package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.UserService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class JCFUserService implements UserService {

   private final static HashMap<UUID, User> userHashMap= new HashMap<>();

   @Override
   public void save(User user) {
      System.out.println("before userHashMap.size: " + userHashMap.size());
      userHashMap.put(user.getId(), user);
      System.out.println("after userHashMap.size: " + userHashMap.size());
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
   public void update(User updateUser) {
      User byId = findById(updateUser.getId());

      byId.update(updateUser);

      userHashMap.put(byId.getId(), byId);
   }

   @Override
   public void delete(UUID userId) {
      User user = userHashMap.get(userId);
      if(user != null) userHashMap.remove(user.getId());
   }
}
