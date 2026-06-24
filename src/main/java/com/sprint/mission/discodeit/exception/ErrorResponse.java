package com.sprint.mission.discodeit.exception;

import java.util.Map;

public record ErrorResponse (
        String message, Map<String,String> fields
){
  public static ErrorResponse of(String message){
      return new ErrorResponse(message, null);
  }

    public static ErrorResponse of(String message, Map<String,String> fields){
        return new ErrorResponse(message, fields);
    }
}
