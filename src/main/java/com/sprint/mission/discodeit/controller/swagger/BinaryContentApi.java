package com.sprint.mission.discodeit.controller.swagger;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Tag(name = "BinaryContent", description = "첨부 파일 API")
public interface BinaryContentApi {

    @Operation(summary = "여러 첨부 파일 조회")
    @ApiResponse(responseCode = "200", description = "첨부 파일 목록 조회 성공")
    ResponseEntity<List<BinaryContentDto>> listByIds(
            @Parameter(description = "조회할 첨부 파일 ID 목록", required = true)
            List<UUID> binaryContentIds
    );

    @Operation(summary = "첨부 파일 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "첨부 파일 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "첨부 파일을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"첨부 파일이 존재하지 않습니다.\",\"fields\":null}")
                    )
            )
    })
    ResponseEntity<BinaryContentDto> findById(
            @Parameter(description = "조회할 첨부 파일 ID", required = true)
            UUID binaryContentId
    );

    @Operation(summary = "파일 다운로드")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "파일 다운로드 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "첨부 파일을 찾을 수 없음"
            )
    })
    ResponseEntity<Resource> download(
            @Parameter(description = "다운로드할 파일 ID", required = true)
            UUID binaryContentId
    );
}
