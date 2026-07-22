package com.sprint.mission.discodeit.dto.request;

public class ValidationMessage {
    public static final String CHANNEL_ID_MESSAGE = "채널 ID는 필수입니다.";
    public static final String CHANNEL_NAME_MESSAGE = "채널 이름은 필수입니다.";

    public static final String USER_ID_MESSAGE = "사용자 ID는 필수입니다.";
    public static final String USER_ID_UNIQUE_MESSAGE = "중복된 사용자가 포함되어 있습니다.";
    public static final String USER_NAME = "사용자명은 필수입니다.";
    public static final String USER_EMAIL = "올바른 이메일 형식이 아닙니다.";
    public static final String USER_EMAIL_REQUIRED_MESSAGE = "이메일은 필수입니다.";
    public static final String USER_PASSWORD = "비밀번호는 필수입니다.";

    public static final String MESSAGE_CONTENT = "메시지 내용은 필수입니다.";

    public static final String TIME_MESSAGE = "시간 정보는 필수입니다.";

    public static final String NOT_NULL_MESSAGE = "값은 비어 있을 수 없습니다.";
    public static final String PARTICIPANTS_SIZE_MESSAGE = "참여자는 2명 이상이어야 합니다.";

    private ValidationMessage() {}
}
