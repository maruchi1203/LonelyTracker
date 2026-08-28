import { describe, expect, it } from 'vitest'
import type { ScheduleResponse } from '../types/schedule'
import {
  groupByStartDate,
  isPostponed,
  occurrenceKey,
  replaceOccurrence,
  sameOccurrence,
} from './occurrence'

/** 반복 일정 하나가 펼쳐져 온 회차. id 가 같고 occurrenceDate 만 다르다 */
function occurrence(
  id: number,
  occurrenceDate: string,
  overrides: Partial<ScheduleResponse> = {},
): ScheduleResponse {
  return {
    id,
    occurrenceDate,
    title: '운동',
    startAt: `${occurrenceDate}T07:00:00`,
    allDay: false,
    status: 'PLANNED',
    postponeCount: 0,
    createdAt: '2026-08-01T00:00:00',
    updatedAt: '2026-08-01T00:00:00',
    ...overrides,
  }
}

describe('회차 식별', () => {
  it('같은 id 라도 날짜가 다르면 다른 회차다', () => {
    const monday = occurrence(1, '2026-08-31')
    const wednesday = occurrence(1, '2026-09-02')

    expect(occurrenceKey(monday)).not.toBe(occurrenceKey(wednesday))
    expect(sameOccurrence(monday, wednesday)).toBe(false)
    expect(sameOccurrence(monday, occurrence(1, '2026-08-31'))).toBe(true)
  })

  it('id 가 다르면 다른 회차다', () => {
    expect(occurrenceKey(occurrence(1, '2026-08-31'))).not.toBe(
      occurrenceKey(occurrence(2, '2026-08-31')),
    )
  })
})

describe('회차 하나만 갈아끼우기', () => {
  it('같은 id 의 다른 회차는 건드리지 않는다', () => {
    const list = [
      occurrence(1, '2026-08-31'),
      occurrence(1, '2026-09-02'),
      occurrence(1, '2026-09-04'),
    ]

    const next = replaceOccurrence(list, occurrence(1, '2026-09-02', { status: 'DONE' }))

    expect(next.map((o) => o.status)).toEqual(['PLANNED', 'DONE', 'PLANNED'])
  })

  it('없는 회차를 넣어도 목록이 늘지 않는다', () => {
    const list = [occurrence(1, '2026-08-31')]

    expect(replaceOccurrence(list, occurrence(9, '2026-08-31'))).toEqual(list)
  })
})

describe('달력 칸에 담기', () => {
  it('원래 날짜가 아니라 실제 시작일로 묶는다', () => {
    // 8/31 예정이던 회차를 9/1 로 미뤘다. 칸은 9/1 에 놓여야 한다
    const postponed = occurrence(1, '2026-08-31', {
      startAt: '2026-09-01T07:00:00',
      postponeCount: 1,
    })

    const grouped = groupByStartDate([postponed])

    expect(grouped.get('2026-09-01')).toHaveLength(1)
    expect(grouped.has('2026-08-31')).toBe(false)
  })

  it('같은 날의 회차를 한 칸에 모은다', () => {
    const grouped = groupByStartDate([
      occurrence(1, '2026-08-31'),
      occurrence(2, '2026-08-31'),
      occurrence(3, '2026-09-01'),
    ])

    expect(grouped.get('2026-08-31')).toHaveLength(2)
    expect(grouped.get('2026-09-01')).toHaveLength(1)
  })
})

describe('연기 여부', () => {
  it('원래 날짜와 시작일이 다르면 연기된 것이다', () => {
    expect(isPostponed(occurrence(1, '2026-08-31'))).toBe(false)
    expect(
      isPostponed(occurrence(1, '2026-08-31', { startAt: '2026-09-01T07:00:00' })),
    ).toBe(true)
  })
})
