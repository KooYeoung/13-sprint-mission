package com.sprint.mission.discodeit.controller.swagger;

import com.sprint.mission.discodeit.dto.request.user.UserCreateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "User 생성 multipart 요청")
public class UserCreateMultipartRequest {

    @Schema(
            description = "User 생성 정보",
            implementation = UserCreateRequest.class,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public UserCreateRequest userCreateRequest;

    @Schema(
            description = "User 프로필 이미지",
            type = "string",
            format = "binary"
    )
    public MultipartFile profile;
}