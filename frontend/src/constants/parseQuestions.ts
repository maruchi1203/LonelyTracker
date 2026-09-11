import type { FormFieldId } from '../domain/scheduleForm'
import type { ParseQuestion } from '../types/parse'

/** 서버는 질문 ID만 보낸다. 사용자가 읽을 문구는 화면이 갖는다 */
export const PARSE_QUESTION_TEXT: Record<ParseQuestion, string> = {
  START_TIME: '몇 시에 시작하실 건가요?',
  DATE: '어느 날짜로 할까요?',
  PLACE: '어디서 하실 건가요?',
  WEEKDAY: '무슨 요일에 반복할까요?',
  RECUR_END: '언제까지 이어갈까요? 정하지 않으면 계속됩니다.',
  TOO_VAGUE: '2분 안에 시작할 수 있는 행동으로 쪼개볼까요?',
  TAG: '어떤 태그를 붙일까요?',
}

/** 질문을 누르면 옮겨갈 입력칸 */
export const PARSE_QUESTION_FIELD: Record<ParseQuestion, FormFieldId> = {
  START_TIME: 'startTime',
  DATE: 'startDate',
  PLACE: 'place',
  WEEKDAY: 'byWeekday',
  RECUR_END: 'endDate',
  TOO_VAGUE: 'title',
  TAG: 'tags',
}

/**
 * 폼에 칸이 있는 질문
 * 장소와 반복 종료일은 폼에서 빠졌다. 눌러도 갈 곳이 없으면 묻지 않는 편이 낫다
 */
const ANSWERABLE: ParseQuestion[] = [
  'START_TIME',
  'DATE',
  'WEEKDAY',
  'TOO_VAGUE',
  'TAG',
]

/** 백엔드가 enum 을 늘려도 화면이 깨지지 않게 답할 수 있는 것만 남긴다 */
export function knownQuestions(questions: string[] | undefined): ParseQuestion[] {
  return (questions ?? []).filter((q): q is ParseQuestion =>
    ANSWERABLE.includes(q as ParseQuestion),
  )
}
