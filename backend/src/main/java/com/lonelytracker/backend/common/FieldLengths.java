package com.lonelytracker.backend.common;

/**
 * 필드 길이 제한을 한 곳에 모은다.
 * <p>
 * 엔티티의 {@code @Column(length = ...)} 과 요청 DTO의 {@code @Size(max = ...)} 는
 * 반드시 같은 값이어야 한다. 어긋나면 검증은 통과하는데 DB 저장에서 터진다.
 * 실제로 DTO가 255자를 허용하는데 컬럼은 100자였던 적이 있다.
 * <p>
 * 이 값을 바꾸면 마이그레이션도 함께 써야 한다. 상수만 고치면 스키마와 어긋나
 * {@code ddl-auto: validate} 가 기동을 막는다.
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

    public static final int DISPLAY_NAME = 50;

    /** #RRGGBB 형식을 기대하지만 강제하지는 않는다 */
    public static final int COLOR = 20;

    private FieldLengths() {
    }
}
