package com.lonelytracker.backend.ai;

/**
 * 파싱이 채우지 못한 칸을 사용자에게 되묻는 항목.
 * <p>
 * 모델은 <b>ID 만</b> 고르고 문구는 서버가 갖는다. 이유가 셋이다.
 * <ol>
 *   <li>JSON 스키마에 enum 으로 걸면 <b>모델이 질문을 지어낼 수 없다</b></li>
 *   <li>문구를 고치거나 다국어를 붙일 때 프롬프트를 안 건드린다</li>
 *   <li>출력 토큰이 줄어 응답이 빨라진다</li>
 * </ol>
 * <p>
 * 문구는 습관 형성 지침(2분 법칙)을 따른다 — 목표가 막연하면 2분 행동으로 쪼개도록
 * 유도하고, 시간·장소가 비면 채워 넣도록 묻는다.
 */
public enum ParseQuestion {

    START_TIME("몇 시에 시작하실 건가요?"),
    DATE("어느 날짜로 할까요?"),
    /** 실행 의도의 절반. 장소가 구체적일수록 실행 확률이 올라간다 */
    PLACE("어디서 하실 건가요?"),
    /** 반복인 듯한데 요일을 못 정한 경우 */
    WEEKDAY("무슨 요일에 반복할까요?"),
    /** 무기한 반복으로 잡혔을 때 끝을 정할지 묻는다 */
    RECUR_END("언제까지 이어갈까요? 정하지 않으면 계속됩니다."),
    /** 행동이 막연할 때. 2분 법칙의 핵심 질문 */
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
