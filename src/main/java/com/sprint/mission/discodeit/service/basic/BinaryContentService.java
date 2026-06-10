package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BinaryContentService {
   private final BinaryContentRepository binaryContentRepository;

   public BinaryContentDto create(BinaryContentDto binaryContentDto){

      BinaryContent binaryContent = new BinaryContent(
              binaryContentDto.originalFileName()
              ,binaryContentDto.contentType()
      );

      BinaryContent save = binaryContentRepository.save(binaryContent);

      return BinaryContentDto.from(save);
   }

   public BinaryContentDto findById(UUID id){

      BinaryContent binaryContent = binaryContentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재 하지 않는 파일입니다."));
      return BinaryContentDto.from(binaryContent);
   }

   public List<BinaryContentDto> findAllByIdIn(List<UUID> ids){

      return binaryContentRepository
            .findAllByIdIn(ids)
            .stream()
            .map(BinaryContentDto::from)
            .toList();
   }

   public void delete(UUID id){

      Optional<BinaryContent> existingContent = binaryContentRepository.findById(id);
      if(existingContent.isPresent()){
         binaryContentRepository.delete(id);
      }

   }

}
