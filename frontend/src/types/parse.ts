// POST /api/schedules/parse 의 응답. 저장되지 않은 초안이다.

import type { RecurrenceFreq, Weekday } from './schedule'

/** AI가 채우지 못한 칸을 알리는 질문 ID. 문구는 프론트가 갖는다 */
export type ParseQuestion =
  | 'START_TIME'
  | 'DATE'
  | 'PLACE'
  | 'WEEKDAY'
  | 'RECUR_END'
  | 'TOO_VAGUE'
  | 'CATEGORY'

export interface ParsedRecurringSchedule {
  freq: RecurrenceFreq
  byWeekday?: Weekday[]
  endsOn?: string
}

/** 백엔드가 null 필드를 아예 빼고 보내므로 채우지 못한 칸은 undefined 다 */
export interface ParsedSchedule {
  title: string
  startAt?: string
  endAt?: string
  allDay: boolean
  category?: string
  place?: string
  recurrence?: ParsedRecurringSchedule
  questions?: ParseQuestion[]
}
