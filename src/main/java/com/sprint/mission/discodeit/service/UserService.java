package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
   void save(User user);
   User findById(UUID userId);
   List<User> findAll();
   void update(UUID userId, String nickname, String realName, String password, String email, String phoneNumber);
   void delete(UUID userId);
}
