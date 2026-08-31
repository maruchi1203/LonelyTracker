import type { ParsedSchedule } from '../types/parse'
import type {
  RecurrenceFreq,
  ScheduleCreateRequest,
  Weekday,
} from '../types/schedule'
import { toLocalDate } from '../utils/datetime'

/** 백엔드에 아직 MONTHLY 가 없다. 화면에서 자리만 잡아두고 보내지 않는다 */
export type FormFreq = RecurrenceFreq | 'MONTHLY'

export type FormFieldId =
  | 'title'
  | 'freq'
  | 'category'
  | 'startDate'
  | 'startTime'
  | 'endDate'
  | 'endTime'
  | 'byWeekday'
  | 'byMonthDay'
  | 'place'

/**
 * 입력 폼이 다루는 모델. 날짜와 시각을 따로 둔다.
 *
 * 시각을 비우면 하루 종일이고, 종료일자 한 칸이 모드에 따라 다른 곳으로 간다 —
 * 한번만이면 일정이 끝나는 날, 반복이면 반복이 끝나는 날이다.
 */
export interface ScheduleForm {
  title: string
  repeating: boolean
  freq: FormFreq
  category: string
  /** "YYYY-MM-DD" */
  startDate: string
  /** "HH:mm". 비면 하루 종일 */
  startTime: string
  endDate: string
  endTime: string
  byWeekday: Weekday[]
  /** MONTHLY 용. 백엔드가 받기 전까지 쓰이지 않는다 */
  byMonthDay: number[]
}

/** AI 초안. 폼에 장소 두 칸이 더 붙는다 */
export interface ScheduleDraft extends ScheduleForm {
  /** 저장할 칸이 아직 없다. 화면 표시와 메모 덧붙이기에만 쓴다 */
  place: string
  keepPlaceInDescription: boolean
}

/** 지금 이후의 가장 가까운 정각 */
function nextHourTime(): string {
  const at = new Date()
  at.setHours(at.getHours() + 1, 0, 0, 0)
  return `${String(at.getHours()).padStart(2, '0')}:00`
}

export function emptyForm(defaultDate?: Date | null): ScheduleForm {
  return {
    title: '',
    repeating: false,
    freq: 'WEEKLY',
    category: '',
    startDate: toLocalDate(defaultDate ?? new Date()),
    startTime: nextHourTime(),
    endDate: '',
    endTime: '',
    byWeekday: [],
    byMonthDay: [],
  }
}

const splitDateTime = (value: string | undefined): [string, string] =>
  value ? [value.slice(0, 10), value.slice(11, 16)] : ['', '']

export function draftFromParsed(
  parsed: ParsedSchedule,
  fallbackDate: Date | null,
): ScheduleDraft {
  const base = emptyForm(fallbackDate)
  const [startDate, startTime] = splitDateTime(parsed.startAt)
  const [endDate, endTime] = splitDateTime(parsed.endAt)

  return {
    ...base,
    title: parsed.title,
    category: parsed.category ?? '',
    // 시작일을 비워두지 않는다. 못 채운 칸은 되물음이 따로 알려준다
    startDate: startDate || base.startDate,
    startTime: parsed.allDay ? '' : startTime || base.startTime,
    // 반복이면 종료일자 칸은 반복이 끝나는 날을 뜻한다
    endDate: parsed.recurrence ? (parsed.recurrence.endsOn ?? '') : endDate,
    endTime,
    repeating: Boolean(parsed.recurrence),
    freq: parsed.recurrence?.freq ?? base.freq,
    byWeekday: parsed.recurrence?.byWeekday ?? [],
    place: parsed.place ?? '',
    keepPlaceInDescription: false,
  }
}

/** 저장 전에 막을 것. 서버 400 을 보고 나서야 알게 되면 늦다 */
export function formValidationError(form: ScheduleForm): string | null {
  if (!form.title.trim()) return '제목을 채워 주세요.'
  if (!form.startDate) return '시작일자를 채워 주세요.'

  if (form.repeating) {
    if (form.freq === 'MONTHLY') return '매월 반복은 아직 준비 중입니다.'
    if (form.freq === 'WEEKLY' && form.byWeekday.length === 0) {
      return '반복할 요일을 하나 이상 골라 주세요.'
    }
    if (form.endDate && form.endDate < form.startDate) {
      return '반복 종료일이 시작일보다 앞설 수 없습니다.'
    }
  } else if (form.endDate && form.endDate < form.startDate) {
    return '종료일자가 시작일자보다 앞설 수 없습니다.'
  }

  if (
    !form.repeating &&
    form.startTime &&
    form.endTime &&
    (form.endDate || form.startDate) === form.startDate &&
    form.endTime < form.startTime
  ) {
    return '종료시각이 시작시각보다 앞설 수 없습니다.'
  }

  return null
}

export function formToCreateRequest(form: ScheduleForm): ScheduleCreateRequest {
  const allDay = !form.startTime
  const startAt = `${form.startDate}T${form.startTime || '00:00'}:00`

  return {
    title: form.title.trim(),
    startAt,
    endAt: endAtOf(form),
    allDay,
    category: form.category.trim() || undefined,
    recurrence: form.repeating
      ? {
          // MONTHLY 는 검증에서 걸러진다
          freq: form.freq as RecurrenceFreq,
          byWeekday: form.freq === 'WEEKLY' ? form.byWeekday : undefined,
          endsOn: form.endDate || undefined,
        }
      : undefined,
  }
}

/**
 * 반복 일정의 종료시각은 시작일자에 얹는다.
 * 백엔드가 이걸 duration_minutes 로 바꿔 저장하므로, 회차마다 날짜가 다른 상황에서
 * 절대 종료시각은 첫 회차에만 맞는다.
 */
function endAtOf(form: ScheduleForm): string | undefined {
  if (form.repeating) {
    return form.endTime ? `${form.startDate}T${form.endTime}:00` : undefined
  }

  if (!form.endDate && !form.endTime) return undefined

  // 종료시각을 안 적었으면 그날 끝까지로 본다
  return `${form.endDate || form.startDate}T${form.endTime || '23:59'}:00`
}

export function draftToCreateRequest(draft: ScheduleDraft): ScheduleCreateRequest {
  const body = formToCreateRequest(draft)

  return {
    ...body,
    description:
      draft.keepPlaceInDescription && draft.place.trim()
        ? `장소: ${draft.place.trim()}`
        : undefined,
  }
}
