package com.sprint.mission.discodeit.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "discodeit.async")
public record AsyncProperties(
        @Valid @NotNull(message = "알림 실행기 설정은 필수입니다.")
        PoolProperties notification,
        @Valid @NotNull(message = "파일 업로드 실행기 설정은 필수입니다.")
        PoolProperties fileUpload
) {
    public record PoolProperties(
            @NotNull(message = "core-pool-size는 필수입니다.")
            @Min(value = 1, message = "core-pool-size는 1 이상이어야 합니다.")
            Integer corePoolSize,
            @NotNull(message = "max-pool-size는 필수입니다.")
            @Min(value = 1, message = "max-pool-size는 1 이상이어야 합니다.")
            Integer maxPoolSize,
            @NotNull(message = "queue-capacity는 필수입니다.")
            @Min(value = 0, message = "queue-capacity는 0 이상이어야 합니다.")
            Integer queueCapacity,
            @NotNull(message = "keep-alive는 필수입니다.")
            Duration keepAlive
    ) {
        @AssertTrue(message = "max-pool-size는 core-pool-size 이상이어야 합니다.")
        public boolean isPoolSizeRangeValid() {
            return corePoolSize == null || maxPoolSize == null || maxPoolSize >= corePoolSize;
        }

        @AssertTrue(message = "keep-alive는 1초 이상 2147483647초 이하의 정수 초여야 합니다.")
        public boolean isKeepAliveValid() {
            return keepAlive == null || (keepAlive.getNano() == 0
                    && keepAlive.getSeconds() >= 1 && keepAlive.getSeconds() <= Integer.MAX_VALUE);
        }
    }
}
