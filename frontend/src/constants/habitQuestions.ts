import type { HabitQuestion } from "../types/habit";

/** 서버는 질문 ID만 보낸다. 사용자가 읽을 문구는 화면이 갖는다 */
export const HABIT_QUESTION_TEXT: Record<HabitQuestion, string> = {
  CATEGORY: "어느 카테고리에 둘까요?",
  CUE_TIME: '언제 하실 건가요? "퇴근 후" 처럼 상황으로 적어도 됩니다.',
  PLACE: "어디서 하실 건가요?",
  TWO_MINUTE: "2분 안에 끝나는 첫 동작은 무엇일까요?",
  TOO_VAGUE: "2분 안에 시작할 수 있는 행동으로 쪼개볼까요?",
};

/** 초안 카드의 어느 칸을 가리키는 질문인지 */
export const HABIT_QUESTION_FIELD: Record<HabitQuestion, string> = {
  CATEGORY: "categoryId",
  CUE_TIME: "atTime",
  PLACE: "place",
  TWO_MINUTE: "twoMinuteAction",
  TOO_VAGUE: "title",
};

/** 초안 카드에 칸이 있는 질문 */
const ANSWERABLE: ReadonlySet<string> = new Set<HabitQuestion>([
  "CATEGORY",
  "CUE_TIME",
  "PLACE",
  "TWO_MINUTE",
  "TOO_VAGUE",
]);

/** 백엔드가 enum 을 늘려도 화면이 깨지지 않게 답할 수 있는 것만 남긴다 */
export function knownHabitQuestions(
  questions: string[] | undefined,
): HabitQuestion[] {
  return (questions ?? []).filter((q): q is HabitQuestion => ANSWERABLE.has(q));
}
