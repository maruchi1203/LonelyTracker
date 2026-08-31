import { describe, expect, it } from 'vitest'
import type { ScheduleResponse } from '../types/schedule'
import {
  coversDate,
  formatOccurrenceRange,
  groupByDate,
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

    const grouped = groupByDate([postponed])

    expect(grouped.get('2026-09-01')).toHaveLength(1)
    expect(grouped.has('2026-08-31')).toBe(false)
  })

  it('같은 날의 회차를 한 칸에 모은다', () => {
    const grouped = groupByDate([
      occurrence(1, '2026-08-31'),
      occurrence(2, '2026-08-31'),
      occurrence(3, '2026-09-01'),
    ])

    expect(grouped.get('2026-08-31')).toHaveLength(2)
    expect(grouped.get('2026-09-01')).toHaveLength(1)
  })

  it('여러 날에 걸친 일정은 걸친 날마다 놓는다', () => {
    const trip = occurrence(1, '2026-08-31', {
      startAt: '2026-08-31T09:00:00',
      endAt: '2026-09-02T18:00:00',
    })

    const grouped = groupByDate([trip])

    expect([...grouped.keys()].sort()).toEqual([
      '2026-08-31',
      '2026-09-01',
      '2026-09-02',
    ])
  })

  it('종료가 같은 날이면 한 칸에만 놓는다', () => {
    const grouped = groupByDate([
      occurrence(1, '2026-08-31', {
        startAt: '2026-08-31T09:00:00',
        endAt: '2026-08-31T18:00:00',
      }),
    ])

    expect(grouped.size).toBe(1)
  })

  it('종료가 시작보다 앞서도 시작일에서 사라지지 않는다', () => {
    const broken = occurrence(1, '2026-08-31', {
      startAt: '2026-08-31T09:00:00',
      endAt: '2026-08-30T18:00:00',
    })

    expect(groupByDate([broken]).get('2026-08-31')).toHaveLength(1)
  })
})

describe('그 날짜에 걸쳐 있는지', () => {
  const trip = occurrence(1, '2026-08-31', {
    startAt: '2026-08-31T09:00:00',
    endAt: '2026-09-02T18:00:00',
  })

  it('첫날뿐 아니라 걸친 날 모두에서 잡힌다', () => {
    // 달력에서 둘째 날을 눌렀을 때 목록이 비면 안 된다
    expect(coversDate(trip, new Date(2026, 7, 31))).toBe(true)
    expect(coversDate(trip, new Date(2026, 8, 1))).toBe(true)
    expect(coversDate(trip, new Date(2026, 8, 2))).toBe(true)
  })

  it('기간 밖은 잡히지 않는다', () => {
    expect(coversDate(trip, new Date(2026, 7, 30))).toBe(false)
    expect(coversDate(trip, new Date(2026, 8, 3))).toBe(false)
  })

  it('하루짜리는 그날만 잡힌다', () => {
    const single = occurrence(2, '2026-08-31')

    expect(coversDate(single, new Date(2026, 7, 31))).toBe(true)
    expect(coversDate(single, new Date(2026, 8, 1))).toBe(false)
  })

  it('연기된 회차는 원래 날짜가 아니라 옮겨간 날에서 잡힌다', () => {
    const postponed = occurrence(3, '2026-08-31', {
      startAt: '2026-09-01T07:00:00',
    })

    expect(coversDate(postponed, new Date(2026, 7, 31))).toBe(false)
    expect(coversDate(postponed, new Date(2026, 8, 1))).toBe(true)
  })

  it('달력이 칸에 놓는 기준과 같다', () => {
    const keys = [...groupByDate([trip]).keys()].sort()
    const covered = ['2026-08-31', '2026-09-01', '2026-09-02'].filter((_, i) =>
      coversDate(trip, new Date(2026, 7, 31 + i)),
    )

    expect(covered).toEqual(keys)
  })
})

describe('기간 문구', () => {
  it('같은 날이면 시각만 이어 붙인다', () => {
    const text = formatOccurrenceRange(
      occurrence(1, '2026-08-31', {
        startAt: '2026-08-31T09:00:00',
        endAt: '2026-08-31T18:30:00',
      }),
    )

    expect(text).toBe('8/31 09:00 ~ 18:30')
  })

  it('날짜가 넘어가면 종료일자도 적는다', () => {
    const text = formatOccurrenceRange(
      occurrence(1, '2026-08-31', {
        startAt: '2026-08-31T09:00:00',
        endAt: '2026-09-02T18:00:00',
      }),
    )

    expect(text).toBe('8/31 09:00 ~ 9/2 18:00')
  })

  it('종료가 없으면 시작만 적는다', () => {
    expect(formatOccurrenceRange(occurrence(1, '2026-08-31'))).toBe('8/31 07:00')
  })

  it('하루 종일이면 시각을 적지 않는다', () => {
    const oneDay = occurrence(1, '2026-08-31', {
      allDay: true,
      startAt: '2026-08-31T00:00:00',
    })
    const span = occurrence(2, '2026-08-31', {
      allDay: true,
      startAt: '2026-08-31T00:00:00',
      endAt: '2026-09-02T23:59:00',
    })

    expect(formatOccurrenceRange(oneDay)).toBe('8/31')
    expect(formatOccurrenceRange(span)).toBe('8/31 ~ 9/2')
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
