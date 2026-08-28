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
    CATEGORY("어느 분류에 넣을까요?");

    private final String text;

    ParseQuestion(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
