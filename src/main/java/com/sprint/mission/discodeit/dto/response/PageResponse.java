package com.sprint.mission.discodeit.dto.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        boolean hasNext,
        Long totalElements
){

    public static <T,S> PageResponse<S> from(Page<T> page, List<S> content){
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.getTotalElements()
        );
    }

    public static <T,S> PageResponse<S> from(Slice<T> slice,  List<S> content){
        return new PageResponse<>(
                content,
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext(),
                null
        );
    }
}
