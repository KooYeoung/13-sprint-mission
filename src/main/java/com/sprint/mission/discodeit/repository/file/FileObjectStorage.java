package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.BaseEntity;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
public class FileObjectStorage <T extends BaseEntity>{
   private final Path path;

   public FileObjectStorage(Path directoryPath){

      this.path = directoryPath;
      // 저장할 경로의 파일 초기화
      if (!Files.exists(path)) {
         try {
            Files.createDirectories(path);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

   }

   public void save(T entity){

      Path filePath = getFilePath(entity.getId());

      try(FileOutputStream fos = new FileOutputStream(filePath.toFile());
          ObjectOutputStream oos = new ObjectOutputStream(fos);
      ){
         oos.writeObject(entity);
      }catch (IOException e){
         log.error("file save error ",e);
      }
   }

   private Path getFilePath(UUID id){
      return path.resolve(id + ".ser");
   }

   public T load(UUID id){
      Path filePath = getFilePath(id);

      try(
            FileInputStream fis = new FileInputStream(filePath.toFile());
            ObjectInputStream ois = new ObjectInputStream(fis)
      ) {
         Object data = ois.readObject();

         return (T) data;
      }catch (IOException | ClassNotFoundException e){
         log.error("file load error ",e);
         return null;
      }
   }

   public List<T> loadAll(){
      try {
         return Files.list(path)
               .map(path ->{
                  try(
                        FileInputStream fis = new FileInputStream(path.toFile());
                        ObjectInputStream ois = new ObjectInputStream(fis)
                  ) {
                     Object data = ois.readObject();
                     return (T) data;
                  }catch (IOException | ClassNotFoundException e){
                     log.error("file loadAll mapping error ",e);
                     return null;
                  }
               }).toList();
      } catch (IOException e) {
         log.error("file loadAll error ",e);
         throw new RuntimeException(e);
      }

   }

   public void delete(UUID id){
      Path filePath = getFilePath(id);
       try {
           Files.deleteIfExists(filePath);
       } catch (IOException e) {
          log.error("file delete error ",e);
           throw new RuntimeException(e);
       }

   }


}
