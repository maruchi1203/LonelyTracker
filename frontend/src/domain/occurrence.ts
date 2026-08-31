import type { ScheduleResponse } from '../types/schedule'
import { formatTime, isSameDay, toLocalDate } from '../utils/datetime'

/**
 * 회차의 식별자. 반복 일정 하나가 여러 회차로 펼쳐져 오므로 id 만으로는 구분되지 않는다.
 * React key, 조회, 갱신이 모두 이 값을 쓴다.
 */
export function occurrenceKey(
  occurrence: Pick<ScheduleResponse, 'id' | 'occurrenceDate'>,
): string {
  return `${occurrence.id}:${occurrence.occurrenceDate}`
}

export function sameOccurrence(
  a: Pick<ScheduleResponse, 'id' | 'occurrenceDate'>,
  b: Pick<ScheduleResponse, 'id' | 'occurrenceDate'>,
): boolean {
  return a.id === b.id && a.occurrenceDate === b.occurrenceDate
}

/** 원래 날짜와 실제 시작일이 다르면 연기된 회차다 */
export function isPostponed(occurrence: ScheduleResponse): boolean {
  return occurrence.occurrenceDate !== toLocalDate(new Date(occurrence.startAt))
}

/** 한 회차가 걸칠 수 있는 날짜 수의 상한. 잘못된 기간이 달력을 망가뜨리지 않게 한다 */
const MAX_SPAN_DAYS = 366

/**
 * 달력 칸에 담는다. 여러 날에 걸친 일정은 걸친 날마다 들어간다.
 *
 * 식별자는 occurrenceDate 지만 화면에 놓이는 자리는 startAt~endAt 이다.
 */
export function groupByDate(
  occurrences: ScheduleResponse[],
): Map<string, ScheduleResponse[]> {
  const map = new Map<string, ScheduleResponse[]>()

  for (const occurrence of occurrences) {
    const start = new Date(occurrence.startAt)
    const finish = occurrence.endAt ? new Date(occurrence.endAt) : start

    const cursor = midnight(start)
    const last = midnight(finish)

    // 종료가 시작보다 앞서 있어도 시작일에는 반드시 한 번 놓는다
    let days = 0
    do {
      const key = toLocalDate(cursor)
      const bucket = map.get(key)
      if (bucket) bucket.push(occurrence)
      else map.set(key, [occurrence])

      cursor.setDate(cursor.getDate() + 1)
      days++
    } while (cursor <= last && days < MAX_SPAN_DAYS)
  }

  return map
}

function midnight(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

/** 목록에 보여줄 기간 문구. 날짜가 넘어가면 종료일자도 함께 적는다 */
export function formatOccurrenceRange(occurrence: ScheduleResponse): string {
  const start = new Date(occurrence.startAt)
  const end = occurrence.endAt ? new Date(occurrence.endAt) : null
  const sameDay = end !== null && isSameDay(start, end)

  if (occurrence.allDay) {
    return !end || sameDay ? day(start) : `${day(start)} ~ ${day(end)}`
  }

  if (!end) return `${day(start)} ${formatTime(start)}`
  if (sameDay) return `${day(start)} ${formatTime(start)} ~ ${formatTime(end)}`
  return `${day(start)} ${formatTime(start)} ~ ${day(end)} ${formatTime(end)}`
}

function day(date: Date): string {
  return `${date.getMonth() + 1}/${date.getDate()}`
}

/** 회차 하나만 바꾼다. 같은 id 의 다른 회차는 건드리지 않는다 */
export function replaceOccurrence(
  occurrences: ScheduleResponse[],
  updated: ScheduleResponse,
): ScheduleResponse[] {
  return occurrences.map((o) => (sameOccurrence(o, updated) ? updated : o))
}
