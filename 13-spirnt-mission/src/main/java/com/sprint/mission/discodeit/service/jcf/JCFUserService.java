package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFUserRepository;
import com.sprint.mission.discodeit.service.UserService;

import java.util.List;
import java.util.UUID;

public class JCFUserService implements UserService {
   private final UserRepository userRepository;

   public JCFUserService(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @Override
   public void save(User user) {
      userRepository.save(user);
   }

   @Override
   public User findById(UUID userId) {

      return userRepository.findById(userId);
   }

   @Override
   public List<User> findAll() {

      return userRepository.findAll();
   }

   @Override
   public void update(UUID userId, String nickname, String realName, String password, String email, String phoneNumber) {
      User byId = findById(userId);
      if(byId == null) return;

      byId.update(nickname
            , realName
            , password
            , email
            , phoneNumber);

      userRepository.save(byId);
   }

   @Override
   public void delete(UUID userId) {
      User user = findById(userId);
      if(user == null) return;

      userRepository.delete(user.getId());
   }
}
