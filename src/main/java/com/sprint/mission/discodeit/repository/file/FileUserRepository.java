package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;

import java.util.List;
import java.util.UUID;

public class FileUserRepository implements UserRepository {
   private final FileObjectStorage<User> storage=
         new FileObjectStorage<>("data/users");

   @Override
   public void save(User user) {
      storage.save(user);
   }

   @Override
   public User findById(UUID id) {
      return storage.load(id);
   }

   @Override
   public List<User> findAll() {
      return storage.loadAll();
   }

   @Override
   public void delete(UUID id) {
      storage.delete(id);
   }
}
