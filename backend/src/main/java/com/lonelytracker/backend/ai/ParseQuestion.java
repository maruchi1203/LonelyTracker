package com.lonelytracker.backend.ai;

/**
 * 파싱이 채우지 못한 칸을 사용자에게 되묻는 항목
 * 문구는 습관 형성 지침(2분 법칙)을 따름
 * 목표가 막연하면 2분 행동으로 쪼개도록 함
 */
public enum ParseQuestion {

    START_TIME("몇 시에 시작하실 건가요?"),
    DATE("어느 날짜로 할까요?"),
    PLACE("어디서 하실 건가요?"),
    WEEKDAY("무슨 요일에 반복할까요?"),
    RECUR_END("언제까지 이어갈까요? 정하지 않으면 계속됩니다."),
    TOO_VAGUE("2분 안에 시작할 수 있는 행동으로 쪼개볼까요?"),
    TAG("어떤 태그를 붙일까요?"),

    // 아래는 습관일지 쪽에서만 온다
    CATEGORY("어느 카테고리에 둘까요?"),
    /** 습관의 신호는 시계가 아니라 상황일 때가 많아 START_TIME 과 따로 묻는다 */
    CUE_TIME("언제 하실 건가요? \"퇴근 후\" 처럼 상황으로 적어도 됩니다."),
    TWO_MINUTE("2분 안에 끝나는 첫 동작은 무엇일까요?");

    private final String text;

    ParseQuestion(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
