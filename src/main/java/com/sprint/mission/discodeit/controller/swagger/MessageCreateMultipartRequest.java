package com.sprint.mission.discodeit.controller.swagger;

import com.sprint.mission.discodeit.dto.request.message.MessageCreateRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "Message 생성 multipart 요청")
public class MessageCreateMultipartRequest {

    @Schema(
            description = "Message 생성 정보",
            implementation = MessageCreateRequest.class,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public MessageCreateRequest messageCreateRequest;

    @ArraySchema(
            arraySchema = @Schema(description = "Message 첨부 파일 목록"),
            schema = @Schema(
                    type = "string",
                    format = "binary"
            )
    )
    public List<MultipartFile> attachments;
}