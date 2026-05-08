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
   public void update(UUID userId, String nickname, String realName, String password, String email, String phoneNumber) {
      User byId = findById(userId);
      if(byId == null) return;

      System.out.println("byId = " + byId);

      byId.update(nickname
            , realName
            , password
            , email
            , phoneNumber);

      // Map에서 조회한 User 객체의 필드 값을 직접 수정했기 때문에,
      // Map에 저장된 객체에도 변경 내용이 이미 반영된다.
      // 단, 갱신 의도를 명확히 하기 위해 동일한 key로 다시 저장한다.

      userHashMap.put(byId.getId(), byId);
   }

   @Override
   public void delete(UUID userId) {
      User user = userHashMap.get(userId);
      if(user != null) userHashMap.remove(user.getId());
   }
}
