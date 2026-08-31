import type { ParsedSchedule } from '../types/parse'
import type {
  RecurrenceFreq,
  RecurrenceRequest,
  ScheduleCreateRequest,
  Weekday,
} from '../types/schedule'
import { nextHour, toLocalInputValue } from '../utils/datetime'

export type DraftFieldId =
  | 'title'
  | 'startAt'
  | 'endAt'
  | 'category'
  | 'place'
  | 'byWeekday'
  | 'endsOn'

/** 반복 부분만. AI 확인 카드와 수동 입력 폼이 함께 쓴다 */
export interface RecurrenceDraft {
  recurring: boolean
  freq: RecurrenceFreq
  byWeekday: Weekday[]
  /** "YYYY-MM-DD". 비면 무기한 */
  endsOn: string
}

export const NO_RECURRENCE: RecurrenceDraft = {
  recurring: false,
  freq: 'WEEKLY',
  byWeekday: [],
  endsOn: '',
}

/** 확인 카드가 다루는 편집용 모델. 입력칸에 그대로 꽂히는 평평한 문자열들이다 */
export interface Draft extends RecurrenceDraft {
  title: string
  /** datetime-local 형식 */
  startAt: string
  endAt: string
  allDay: boolean
  category: string
  /** 저장할 칸이 아직 없다. 화면 표시와 메모 덧붙이기에만 쓴다 */
  place: string
  keepPlaceInDescription: boolean
}

const toInput = (value: string | undefined, fallback: string): string =>
  value ? toLocalInputValue(new Date(value)) : fallback

export function draftFromParsed(
  parsed: ParsedSchedule,
  fallbackDate: Date | null,
): Draft {
  return {
    title: parsed.title,
    // 시작 시각을 비워두지 않는다. 못 채운 칸은 되물음이 따로 알려준다
    startAt: toInput(parsed.startAt, nextHour(fallbackDate)),
    endAt: toInput(parsed.endAt, ''),
    allDay: parsed.allDay,
    category: parsed.category ?? '',
    place: parsed.place ?? '',
    keepPlaceInDescription: false,
    recurring: Boolean(parsed.recurrence),
    freq: parsed.recurrence?.freq ?? 'WEEKLY',
    byWeekday: parsed.recurrence?.byWeekday ?? [],
    endsOn: parsed.recurrence?.endsOn ?? '',
  }
}

/** 저장 전에 막을 것. 서버 400 을 보고 나서야 알게 되면 늦다 */
export function recurrenceValidationError(value: RecurrenceDraft): string | null {
  if (value.recurring && value.freq === 'WEEKLY' && value.byWeekday.length === 0) {
    return '반복할 요일을 하나 이상 골라 주세요.'
  }
  return null
}

export function draftValidationError(draft: Draft): string | null {
  if (!draft.title.trim()) return '제목을 채워 주세요.'
  if (!draft.startAt) return '시작 시각을 채워 주세요.'
  return recurrenceValidationError(draft)
}

/** 반복이 꺼져 있으면 아예 보내지 않는다. DAILY 는 요일을 쓰지 않는다 */
export function toRecurrenceRequest(
  value: RecurrenceDraft,
): RecurrenceRequest | undefined {
  if (!value.recurring) return undefined

  return {
    freq: value.freq,
    byWeekday: value.freq === 'WEEKLY' ? value.byWeekday : undefined,
    endsOn: value.endsOn || undefined,
  }
}

export function draftToCreateRequest(draft: Draft): ScheduleCreateRequest {
  const description =
    draft.keepPlaceInDescription && draft.place.trim()
      ? `장소: ${draft.place.trim()}`
      : undefined

  return {
    title: draft.title.trim(),
    description,
    startAt: `${draft.startAt}:00`,
    endAt: draft.endAt ? `${draft.endAt}:00` : undefined,
    allDay: draft.allDay,
    category: draft.category.trim() || undefined,
    recurrence: toRecurrenceRequest(draft),
  }
}
