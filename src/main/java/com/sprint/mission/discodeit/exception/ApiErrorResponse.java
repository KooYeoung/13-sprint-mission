package com.sprint.mission.discodeit.exception;

import java.util.Map;

public record ApiErrorResponse(
        String message, Map<String,String> fields
){
  public static ApiErrorResponse of(String message){
      return new ApiErrorResponse(message, null);
  }

    public static ApiErrorResponse of(String message, Map<String,String> fields){
        return new ApiErrorResponse(message, fields);
    }
}
