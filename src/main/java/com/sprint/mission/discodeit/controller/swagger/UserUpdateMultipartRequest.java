package com.sprint.mission.discodeit.controller.swagger;

import com.sprint.mission.discodeit.dto.request.user.UserUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "User 수정 multipart 요청")
public class UserUpdateMultipartRequest {

    @Schema(
            description = "수정할 User 정보",
            implementation = UserUpdateRequest.class,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public UserUpdateRequest userUpdateRequest;

    @Schema(
            description = "수정할 User 프로필 이미지",
            type = "string",
            format = "binary"
    )
    public MultipartFile profile;
}