import type { ScheduleResponse } from '../types/schedule'
import { toLocalDate } from '../utils/datetime'

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

/**
 * 달력 칸에 담기 위해 실제 시작일로 묶는다.
 * 식별자는 occurrenceDate 지만 화면에 놓이는 자리는 startAt 이다.
 */
export function groupByStartDate(
  occurrences: ScheduleResponse[],
): Map<string, ScheduleResponse[]> {
  const map = new Map<string, ScheduleResponse[]>()

  for (const occurrence of occurrences) {
    const key = toLocalDate(new Date(occurrence.startAt))
    const bucket = map.get(key)
    if (bucket) bucket.push(occurrence)
    else map.set(key, [occurrence])
  }

  return map
}

/** 회차 하나만 바꾼다. 같은 id 의 다른 회차는 건드리지 않는다 */
export function replaceOccurrence(
  occurrences: ScheduleResponse[],
  updated: ScheduleResponse,
): ScheduleResponse[] {
  return occurrences.map((o) => (sameOccurrence(o, updated) ? updated : o))
}
