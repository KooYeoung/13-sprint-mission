package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

// sealed : 인터페이스를 구현할 수 있는 타입을 미리 제한하는 Java 기능
// permits : 구현할 수 있는 클래스 지정
public sealed interface DownloadResult
        permits ResourceDownloadResult, RedirectDownloadResult {

    ResponseEntity<Resource> toResponseEntity(BinaryContentDto metadata);
}
