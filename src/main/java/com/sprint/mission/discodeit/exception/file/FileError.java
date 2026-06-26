package com.sprint.mission.discodeit.exception.file;

import lombok.Getter;

@Getter
public enum FileError {
    SAVE("파일 저장 중 오류가 발생했습니다."),
    READ("파일을 읽는 중 오류가 발생했습니다."),
    DIRECTORY("디렉터리 생성 중 오류가 발생했습니다."),
    DELETE("파일 삭제 중 오류가 발생했습니다."),
    NOT_FOUND("존재 하지 않는 파일입니다.");

    private final String message;


    FileError(String message) {
        this.message = message;
    }
}
