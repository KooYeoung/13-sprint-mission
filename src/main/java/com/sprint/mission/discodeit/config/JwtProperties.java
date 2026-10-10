package com.sprint.mission.discodeit.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "discodeit.security.jwt")
public record JwtProperties(
        @DefaultValue("1") @NotNull @Min(1)
        Integer maxActiveJwtCount,
        String secret,
        Duration accessTokenValidity,
        Duration refreshTokenValidity,
        String issuer
) {

}
