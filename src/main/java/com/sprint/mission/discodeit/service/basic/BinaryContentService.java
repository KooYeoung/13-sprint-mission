package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.response.BinaryContentResponse;
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

   public void create(BinaryContentCreateRequest request){
      /**
       * [ ] DTO를 활용해 파라미터를 그룹화합니다.
       */
      BinaryContent binaryContent = request.toBinaryContent();
      binaryContentRepository.save(binaryContent);

   }

   public BinaryContentResponse findById(UUID id){

      BinaryContent binaryContent = binaryContentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재 하지 않는 파일입니다."));
      return BinaryContentResponse.from(binaryContent);
   }

   public List<BinaryContentResponse> findAllByIdIn(List<UUID> ids){

      return binaryContentRepository
            .findAllByIdIn(ids)
            .stream()
            .map(BinaryContentResponse::from)
            .toList();
   }

   public void delete(UUID id){

      Optional<BinaryContent> existingContent = binaryContentRepository.findById(id);
      if(existingContent.isPresent()){
         binaryContentRepository.delete(id);
      }

   }

}
