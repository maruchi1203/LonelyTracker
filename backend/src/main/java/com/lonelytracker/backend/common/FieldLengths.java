package com.lonelytracker.backend.common;

/**
 * 필드 길이 제한을 한 곳에 모은 클래스.
 * 어노테이션 인자는 컴파일 상수여야 해서 yml로 뺄 수 없다.
 */
public final class FieldLengths {

    /** 일정 제목 */
    public static final int TITLE = 200;

    /** 마크다운 원문. 문서처럼 길어질 수 있다 */
    public static final int DESCRIPTION = 20000;

    /** 카테고리 이름, 일정에 기록되는 분류 문자열 */
    public static final int CATEGORY_NAME = 50;

    /** OAuth 로그인 식별자(이메일 등)가 들어온다 */
    public static final int USERNAME = 100;

    /** 화면에 보여줄 이름 */
    public static final int DISPLAY_NAME = 50;

    /** #RRGGBB 형식을 기대하지만 강제하지는 않는다 */
    public static final int COLOR = 20;

    private FieldLengths() {
    }
}
