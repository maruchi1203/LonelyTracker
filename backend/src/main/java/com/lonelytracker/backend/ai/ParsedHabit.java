package com.lonelytracker.backend.ai;

import java.util.List;

/**
 * 자연어에서 뽑아낸 습관 초안.
 *
 * <p>2분 행동이 비면 {@code suggestedAction} 도 내려간다. 깃발만 남으면 화면이 없는 값을
 * "제안"이라고 말한다.
 *
 * @param categoryName    AI 가 고른 카테고리 이름. 고를 목록을 함께 주므로 그 안의 하나다.
 *                        못 고르면 null 이고 questions 에 CATEGORY 가 들어온다
 * @param atTime          언제 할지. "07:00" 도 "퇴근 후" 도 글자 그대로 둔다
 * @param place           어디서 할지
 * @param twoMinuteAction 시작을 여는 2분 이내의 행동
 * @param suggestedAction 2분 행동이 문장에 없어 AI 가 지어낸 것인지.
 *                        화면이 "제안"이라고 밝혀야 사용자가 제 것으로 고쳐 쓴다
 * @param questions       채우지 못한 칸
 */
public record ParsedHabit(
        String title,
        String categoryName,
        String atTime,
        String place,
        String twoMinuteAction,
        boolean suggestedAction,
        List<ParseQuestion> questions) {

    public ParsedHabit {
        if (twoMinuteAction == null || twoMinuteAction.isBlank()) {
            twoMinuteAction = null;
            suggestedAction = false;
        }
    }
}
