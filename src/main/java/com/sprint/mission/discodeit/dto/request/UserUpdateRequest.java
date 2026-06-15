package com.sprint.mission.discodeit.dto.request;

import lombok.With;
import java.util.UUID;

@With
public record UserUpdateRequest(
   UUID userId
   , String nickname
   ,  String realName
   ,  String password
   ,  String email
   ,  String phoneNumber
)
{
}
