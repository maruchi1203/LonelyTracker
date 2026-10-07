package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.ai.ParseQuestion;
import com.lonelytracker.backend.ai.ParsedHabit;

import java.util.List;

/**
 * 읽어낸 습관 초안 한 장.
 *
 * <p>AI 는 카테고리를 이름으로 고르지만 화면은 id 로 저장한다. 그 맞춤을 서버가 해 두는
 * 까닭은, 못 맞춘 경우를 화면이 또 판단하게 두면 같은 규칙이 두 군데 생기기 때문이다.
 *
 * @param categoryId      맞춘 카테고리. 못 맞췄으면 null 이고 questions 에 CATEGORY 가 있다
 * @param suggestedAction twoMinuteAction 이 문장에서 읽은 것이 아니라 AI 가 지어낸 것인지.
 *                        화면이 "제안"이라고 밝혀 사용자가 제 것으로 고치게 한다
 */
public record HabitDraftResponse(
        String title,
        Long categoryId,
        String atTime,
        String place,
        String twoMinuteAction,
        boolean suggestedAction,
        List<ParseQuestion> questions) {

    public static HabitDraftResponse of(ParsedHabit parsed, Long categoryId) {
        return new HabitDraftResponse(
                parsed.title(),
                categoryId,
                parsed.atTime(),
                parsed.place(),
                parsed.twoMinuteAction(),
                parsed.suggestedAction(),
                parsed.questions());
    }
}
