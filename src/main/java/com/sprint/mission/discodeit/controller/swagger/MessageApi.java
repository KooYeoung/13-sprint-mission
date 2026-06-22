package com.sprint.mission.discodeit.controller.swagger;

import com.sprint.mission.discodeit.dto.request.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Message", description = "Message API")
public interface MessageApi {

    @Operation(summary = "Message 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message가 성공적으로 생성됨"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Channel 또는 Author를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"채널이 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<MessageDto> create(
            @Parameter(description = "Message 생성 정보", required = true)
            @RequestPart("messageCreateRequest") MessageCreateRequest messageCreateRequest,

            @Parameter(description = "Message 첨부 파일 목록")
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    );

    @Operation(summary = "Channel의 Message 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Channel을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"채널이 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<List<MessageDto>> listByChannelId(
            @Parameter(description = "조회할 Channel ID", required = true)
            UUID channelId
    );

    @Operation(summary = "Message 내용 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message가 성공적으로 수정됨"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Message를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"메시지가 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<MessageDto> update(
            @Parameter(description = "수정할 Message ID", required = true)
            UUID messageId,

            @RequestBody MessageUpdateRequest request
    );

    @Operation(summary = "Message 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Message가 성공적으로 삭제됨"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Message를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"메시지가 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 Message ID", required = true)
            UUID messageId
    );
}
