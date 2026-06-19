package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.exception.CustomFileNotFoundException;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BinaryContentService {
   private final BinaryContentRepository binaryContentRepository;
   private final Path uploadDir;

   public BinaryContentService(BinaryContentRepository binaryContentRepository) {
      this.binaryContentRepository = binaryContentRepository;
      this.uploadDir = Paths.get("uploads");
      try {
         Files.createDirectories(uploadDir);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public Optional<BinaryContentDto> create(MultipartFile file){
      if(file == null || file.isEmpty()){
         return Optional.empty();
      }

      String originalFileName = file.getOriginalFilename();
      String storedFileName = UUID.randomUUID() + "_" + originalFileName;
      Path savePath = uploadDir.resolve(storedFileName);

      try {
         Files.copy(file.getInputStream(), savePath);
      } catch (IOException e) {
         throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
      }

      BinaryContent binaryContent = new BinaryContent(
              originalFileName,
              storedFileName,
              file.getContentType(),
              file.getSize(),
              savePath.toString()
      );

      BinaryContent save = binaryContentRepository.save(binaryContent);

      return Optional.of(BinaryContentDto.from(save, getBytes(save)));
   }

   public BinaryContentDto findById(UUID id){

      BinaryContent binaryContent = binaryContentRepository.findById(id)
              .orElseThrow(() -> new CustomFileNotFoundException("존재 하지 않는 파일입니다."));

      byte[] bytes = getBytes(binaryContent);

      return BinaryContentDto.from(binaryContent, bytes);
   }

   @NonNull
   private static byte[] getBytes(BinaryContent binaryContent) {
      try {
         return Files.readAllBytes(Path.of(binaryContent.getPath()));
      } catch (IOException e) {
         throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다.", e);
      }
   }

   public List<BinaryContentDto> findAllByIdIn(List<UUID> ids){

      return binaryContentRepository
              .findAllByIdIn(ids)
              .stream()
              .map(b -> BinaryContentDto.from(b , getBytes(b)))
              .toList();
   }

   public void delete(UUID id){

      Optional<BinaryContent> existingContent = binaryContentRepository.findById(id);

      if (existingContent.isEmpty()) {
         return;
      }

      BinaryContent binaryContent = existingContent.get();

      try {
         Files.deleteIfExists(Path.of(binaryContent.getPath()));
      } catch (IOException e) {
         throw new RuntimeException("파일 삭제 중 오류가 발생했습니다.", e);
      }

      binaryContentRepository.delete(id);

   }

}
