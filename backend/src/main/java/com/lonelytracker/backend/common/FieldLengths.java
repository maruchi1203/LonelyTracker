package com.lonelytracker.backend.common;

/**
 * 필드 길이 제한을 한 곳에 모은 클래스
 * 어노테이션 인자는 컴파일 상수여야 해서 yml로 뺄 수 없음
 */
public final class FieldLengths {

    /** 일정 제목 */
    public static final int TITLE = 200;

    /** 마크다운 원문. 문서처럼 길어질 수 있다 */
    public static final int DESCRIPTION = 20000;

    /** 일정에 붙이는 태그 하나 */
    public static final int TAG = 50;

    /** 일정을 수행할 장소 */
    public static final int PLACE = 200;

    /** 시작에 필요한 2분 이내의 미니 행동 */
    public static final int TWO_MINUTE_ACTION = 200;

    /** OAuth 로그인 식별자(이메일 등)가 들어온다 */
    public static final int USERNAME = 100;

    /** 화면에 보여줄 이름 */
    public static final int DISPLAY_NAME = 50;

    /** #RRGGBB 형식을 기대하지만 강제하지는 않는다 */
    public static final int COLOR = 20;

    private FieldLengths() {
    }
}
