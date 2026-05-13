package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.BaseEntity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class FileObjectStorage <T extends BaseEntity>{
   private final Path path;

   public FileObjectStorage(String directoryPath){

      this.path = Path.of(directoryPath);
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

      try(FileOutputStream fos = new FileOutputStream(path +"/"+entity.getId() +".ser");
          ObjectOutputStream oos = new ObjectOutputStream(fos);
      ){
         oos.writeObject(entity);
      }catch (IOException e){
         e.printStackTrace();
      }
   }

   public T load(UUID id){
      try(
            FileInputStream fis = new FileInputStream(path +"/"+id +".ser");
            ObjectInputStream ois = new ObjectInputStream(fis)
      ) {
         Object data = ois.readObject();

         return (T) data;
      }catch (IOException | ClassNotFoundException e){
         e.printStackTrace();
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
                     e.printStackTrace();
                     return null;
                  }
               }).toList();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

   }

   public void delete(UUID id){
      File file = new File(path +"/"+id +".ser");
      file.delete();
   }


}
