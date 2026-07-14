package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class PageResponseMapper<T> {

    public PageResponse<T> fromPage(Page<T> page, Object nextCursor) {

        return new PageResponse<>(
                page.getContent(),
                nextCursor,
                page.getSize(),
                page.hasNext()
        );
    }

    public PageResponse<T> fromSlice(Slice<T> slice, Object nextCursor) {

        return new PageResponse<>(
                slice.getContent(),
                nextCursor,
                slice.getSize(),
                slice.hasNext()
        );
    }

}
