// POST /api/schedules/parse 의 응답. 저장되지 않은 초안이다.

import type { RecurrenceFreq, Weekday } from "./schedule";

/** AI가 필요한 정보에 대한 질문 ID. 문구는 프론트가 갖는다 */
export type ParseQuestion =
  | "START_TIME"
  | "DATE"
  | "PLACE"
  | "WEEKDAY"
  | "RECUR_END"
  | "TOO_VAGUE"
  | "TAG";

export interface ParsedRecurringSchedule {
  freq: RecurrenceFreq;
  byWeekday?: Weekday[];
  endsOn?: string;
}

/** 백엔드가 null 필드를 아예 빼고 보내므로 채우지 못한 칸은 undefined 다 */
export interface ParsedSchedule {
  title: string;
  startAt?: string;
  endAt?: string;
  allDay: boolean;
  tags?: string[];
  place?: string;
  recurrence?: ParsedRecurringSchedule;
  questions?: ParseQuestion[];
}

/** 부르는 데 성공했으면 200 이다. 읽을 것이 없으면 초안 대신 notice 가 온다 */
export interface ParseResponse {
  schedules: ParsedSchedule[];
  notice?: string;
}
