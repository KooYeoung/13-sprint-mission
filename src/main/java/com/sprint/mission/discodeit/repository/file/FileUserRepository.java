package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
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
public class FileUserRepository implements UserRepository {
   private final FileObjectStorage<User> storage=
         new FileObjectStorage<>("data/users");

   @Override
   public void save(User user) {
      storage.save(user);
   }

   @Override
   public Optional<User> findById(UUID id) {

      return Optional.ofNullable(storage.load(id));
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
