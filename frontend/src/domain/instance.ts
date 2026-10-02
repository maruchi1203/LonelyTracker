import type { ScheduleResponse } from '../types/schedule'
import { formatTime, isSameDay, toLocalDate } from '../utils/datetime'

/**
 * 회차의 식별자. 반복 일정 하나가 여러 회차로 펼쳐져 오므로 id 만으로는 구분되지 않는다.
 * React key, 조회, 갱신이 모두 이 값을 쓴다.
 */
export function instanceKey(
  instance: Pick<ScheduleResponse, 'id' | 'instanceDate'>,
): string {
  return `${instance.id}:${instance.instanceDate}`
}

export function sameInstance(
  a: Pick<ScheduleResponse, 'id' | 'instanceDate'>,
  b: Pick<ScheduleResponse, 'id' | 'instanceDate'>,
): boolean {
  return a.id === b.id && a.instanceDate === b.instanceDate
}

/**
 * 원래 날짜가 아닌 날에 놓인 회차인지. 서버가 세지 않고 두 값의 차이로 안다.
 * 날짜만 보므로 같은 날 안에서 시각만 옮긴 것은 해당하지 않는다.
 */
export function isMoved(instance: ScheduleResponse): boolean {
  if (!instance.startAt || !instance.instanceDate) return false
  return instance.instanceDate !== toLocalDate(new Date(instance.startAt))
}

/**
 * 아직 오지 않은 날을 미리 완료한 회차인지. 수행률의 분모에 들어가지 않는다.
 *
 * @param today 기본값은 오늘. 테스트가 고정 날짜를 넣는다
 */
export function isEarlyDone(
  instance: ScheduleResponse,
  today: string = toLocalDate(new Date()),
): boolean {
  if (!instance.instanceDate) return false
  return instance.status === 'DONE' && instance.instanceDate > today
}

/** 한 회차가 걸칠 수 있는 날짜 수의 상한. 잘못된 기간이 달력을 망가뜨리지 않게 한다 */
const MAX_SPAN_DAYS = 366

/**
 * 달력 칸에 담는다. 여러 날에 걸친 일정은 걸친 날마다 들어간다.
 *
 * 식별자는 instanceDate 지만 화면에 놓이는 자리는 startAt~endAt 이다.
 */
export function groupByDate(
  instances: ScheduleResponse[],
): Map<string, ScheduleResponse[]> {
  const map = new Map<string, ScheduleResponse[]>()

  for (const instance of instances) {
    for (const key of instanceDateKeys(instance)) {
      const bucket = map.get(key)
      if (bucket) bucket.push(instance)
      else map.set(key, [instance])
    }
  }

  return map
}

/** 펼친 칸에 세울 줄 하나. 같은 일정이라도 시작과 기한은 따로 선다 */
export interface DayEntry {
  instance: ScheduleResponse
  kind: 'span' | 'due'
}

/**
 * 펼친 칸이 줄로 세울 것들을 날짜별로 모은다.
 *
 * 여러 날에 걸친 일정은 담지 않는다 — 그것은 띠가 맡아야 일곱 칸을 가로질러 이어진다.
 * 기한은 걸치지 않고 하루에만 서므로 걸친 일정의 기한이라도 여기로 온다.
 *
 * 줄 차례는 시각순이고, 기한은 그 날의 끝이라 맨 뒤에 세운다
 */
export function entriesByDate(
  instances: ScheduleResponse[],
): Map<string, DayEntry[]> {
  const spans = new Map<string, DayEntry[]>()
  const dues = new Map<string, DayEntry[]>()

  const push = (map: Map<string, DayEntry[]>, key: string, entry: DayEntry) => {
    const bucket = map.get(key)
    if (bucket) bucket.push(entry)
    else map.set(key, [entry])
  }

  for (const instance of instances) {
    const covered = instanceDateKeys(instance)
    if (covered.length === 1) {
      push(spans, covered[0], { instance, kind: 'span' })
    }
    if (instance.dueOn) {
      push(dues, instance.dueOn, { instance, kind: 'due' })
    }
  }

  const result = new Map<string, DayEntry[]>()
  for (const key of new Set([...spans.keys(), ...dues.keys()])) {
    const ofDay = spans.get(key) ?? []
    const sorted = sortByTimeOfDay(ofDay.map((e) => e.instance))
    result.set(key, [
      ...sorted.map((instance) => ({ instance, kind: 'span' as const })),
      ...(dues.get(key) ?? []),
    ])
  }

  return result
}

/**
 * 하루 안에서의 차례. 하루 종일이 맨 앞에 서고 그 뒤로 시각순이다.
 *
 * 여러 날에 걸친 일정은 뒷날에도 제 시작 시각으로 선다. 그 날 몇 시에 걸렸는지보다
 * 몇 시에 하기로 한 일인지가 눈에 익다
 */
export function sortByTimeOfDay(
  instances: ScheduleResponse[],
): ScheduleResponse[] {
  return [...instances].sort(
    (a, b) =>
      minuteOfDay(a) - minuteOfDay(b) || a.title.localeCompare(b.title, 'ko'),
  )
}

/** 하루 종일이거나 시각이 없으면 -1 이라 맨 앞에 선다 */
function minuteOfDay(instance: ScheduleResponse): number {
  if (instance.allDay || !instance.startAt) return -1
  const at = new Date(instance.startAt)
  return at.getHours() * 60 + at.getMinutes()
}

/** 그 날짜에 걸쳐 있는 회차인지. 달력과 목록이 같은 기준을 써야 한다 */
export function coversDate(instance: ScheduleResponse, date: Date): boolean {
  return instanceDateKeys(instance).includes(toLocalDate(date))
}

/** 회차가 걸치는 날짜들. 시작일은 언제나 하나 들어간다 */
export function instanceDateKeys(instance: ScheduleResponse): string[] {
  // 날짜를 안 정한 항목은 달력의 어느 칸에도 놓이지 않는다
  if (!instance.startAt) return []
  const start = new Date(instance.startAt)
  const finish = instance.endAt ? new Date(instance.endAt) : start

  const cursor = midnight(start)
  const last = midnight(finish)
  const keys: string[] = []

  // 종료가 시작보다 앞서 있어도 시작일에는 반드시 한 번 놓는다
  do {
    keys.push(toLocalDate(cursor))
    cursor.setDate(cursor.getDate() + 1)
  } while (cursor <= last && keys.length < MAX_SPAN_DAYS)

  return keys
}

function midnight(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

/** 목록에 보여줄 기간 문구. 날짜가 넘어가면 종료일자도 함께 적는다 */
export function formatInstanceRange(instance: ScheduleResponse): string {
  if (!instance.startAt) return '날짜 없음'
  const start = new Date(instance.startAt)
  const end = instance.endAt ? new Date(instance.endAt) : null
  const sameDay = end !== null && isSameDay(start, end)

  if (instance.allDay) {
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
export function replaceInstance(
  instances: ScheduleResponse[],
  updated: ScheduleResponse,
): ScheduleResponse[] {
  return instances.map((o) => (sameInstance(o, updated) ? updated : o))
}
