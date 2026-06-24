package com.sprint.mission.discodeit.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum ChannelType {
   PUBLIC, PRIVATE;

   private static final Set<String> AVAILABLE_TYPES = Arrays.stream(ChannelType.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());

   public static ChannelType getChannelType(String type){
      if(type == null || type.isBlank()) throw new IllegalArgumentException("타입이 존재하지 않습니다.");
      String normalizedType = type.trim().toUpperCase();
      if(!AVAILABLE_TYPES.contains(normalizedType)) throw new IllegalArgumentException("지원하지 않는 타입입니다.");
      return ChannelType.valueOf(normalizedType);
   }
}
