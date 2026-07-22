package com.sprint.mission.discodeit.controller.swagger;

import com.sprint.mission.discodeit.dto.request.readStatus.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.readStatus.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "ReadStatus", description = "Message 읽음 상태 API")
public interface ReadStatusApi {


    @Operation(summary = "Message 읽음 상태 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message 읽음 상태가 성공적으로 생성됨"),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 읽음 상태가 존재함",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"이미 읽음 상태가 존재합니다.\",\"fields\":null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Channel 또는 User를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"채널이 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<ReadStatusDto> save( @Valid @RequestBody ReadStatusCreateRequest request);

    @Operation(summary = "User의 Message 읽음 상태 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message 읽음 상태 목록 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "User를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"유저가 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<List<ReadStatusDto>> listByUserId(
            @Parameter(description = "조회할 User ID", required = true)
            UUID userId
    );

    @Operation(summary = "Message 읽음 상태 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message 읽음 상태가 성공적으로 수정됨"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Message 읽음 상태를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"읽음 상태가 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<ReadStatusDto> update(
            @Parameter(description = "수정할 읽음 상태 ID", required = true)
            UUID readStatusId,

            @Valid @RequestBody ReadStatusUpdateRequest request
    );
}
