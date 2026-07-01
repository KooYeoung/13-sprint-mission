package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.config.RepositoryProperties;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
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
public class FileUserStatusRepository implements UserStatusRepository {
   private final FileObjectStorage<UserStatus> storage ;

   public FileUserStatusRepository(RepositoryProperties properties) {
      this. storage = new FileObjectStorage<>(
              properties.getFileDirectory().resolve("user-status")
      );
   }

   @Override
   public UserStatus save(UserStatus userStatus) {
      storage.save(userStatus);
      return userStatus;
   }

   @Override
   public Optional<UserStatus> findByIdAndUserId(UUID userStatusId, UUID userId) {
      UserStatus load = storage.load(userStatusId);
      if(load == null ) return Optional.empty();
      if(!load.getUserId().equals(userId)) return Optional.empty();

      return Optional.of(load);
   }

   @Override
   public Optional<UserStatus> findByUserId(UUID userId) {

      return storage.loadAll()
            .stream()
            .filter(us -> us.getUserId().equals(userId))
            .findFirst();
   }

   @Override
   public List<UserStatus> findAll() {

      return storage.loadAll();
   }

   @Override
   public UserStatus update(UserStatus userStatus) {
      return save(userStatus);
   }

   @Override
   public void delete(UUID id) {
      storage.delete(id);
   }
}
