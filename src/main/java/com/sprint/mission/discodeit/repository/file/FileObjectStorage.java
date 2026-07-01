package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.BaseEntity;
import com.sprint.mission.discodeit.exception.CustomInternalServerException;
import com.sprint.mission.discodeit.exception.file.FileError;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FileObjectStorage <T extends BaseEntity>{
   private final Path path;
   private final Map<UUID, T> dataMap = new ConcurrentHashMap<>();

   public FileObjectStorage(Path directoryPath){

      this.path = directoryPath;

         try {
            Files.createDirectories(path);
             loadAllFromDisk();
         } catch (IOException e) {
             throw new CustomInternalServerException(FileError.DIRECTORY.getMessage(),e);
         }

   }

    private void loadAllFromDisk() throws IOException {
        try (var paths = Files.list(path)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(filePath -> filePath.toString().endsWith(".ser"))
                    .forEach(filePath -> {
                        try (
                                FileInputStream fis = new FileInputStream(filePath.toFile());
                                ObjectInputStream ois = new ObjectInputStream(fis)
                        ) {
                            Object data = ois.readObject();
                            T entity = (T) data;
                            dataMap.put(entity.getId(), entity);
                        } catch (IOException | ClassNotFoundException e) {
                            log.error("file load mapping error. path={}", filePath, e);
                        }
                    });
        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.READ.getMessage(), e);
        }
    }

    public void save(T entity){
      UUID entityId = entity.getId();
      Path filePath = getFilePath(entityId);

      try(FileOutputStream fos = new FileOutputStream(filePath.toFile());
          ObjectOutputStream oos = new ObjectOutputStream(fos);
      ){
         oos.writeObject(entity);
         dataMap.put(entityId,entity);
      }catch (IOException e){
         throw new CustomInternalServerException(FileError.SAVE.getMessage(), e);
      }
   }

   private Path getFilePath(UUID id){
      return path.resolve(id + ".ser");
   }

   public T load(UUID id){
      T t = dataMap.get(id);
      return t;
   }

   public List<T> loadAll(){

      return dataMap.values().stream().toList();
   }

   public void delete(UUID id){
      Path filePath = getFilePath(id);
       try {
           Files.deleteIfExists(filePath);
       } catch (IOException e) {
          throw new CustomInternalServerException(FileError.DELETE.getMessage(), e);
       }
       dataMap.remove(id);
   }


}
