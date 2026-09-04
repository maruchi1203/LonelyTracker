import { describe, expect, it } from 'vitest'
import type { ScheduleResponse } from '../types/schedule'
import {
  coversDate,
  formatInstanceRange,
  groupByDate,
  isEarlyDone,
  isMoved,
  instanceKey,
  replaceInstance,
  sameInstance,
} from './instance'

/** 반복 일정 하나가 펼쳐져 온 회차. id 가 같고 instanceDate 만 다르다 */
function instance(
  id: number,
  instanceDate: string,
  overrides: Partial<ScheduleResponse> = {},
): ScheduleResponse {
  return {
    id,
    instanceDate,
    title: '운동',
    startAt: `${instanceDate}T07:00:00`,
    allDay: false,
    status: 'PLANNED',
      createdAt: '2026-08-01T00:00:00',
    updatedAt: '2026-08-01T00:00:00',
    ...overrides,
  }
}

describe('회차 식별', () => {
  it('같은 id 라도 날짜가 다르면 다른 회차다', () => {
    const monday = instance(1, '2026-08-31')
    const wednesday = instance(1, '2026-09-02')

    expect(instanceKey(monday)).not.toBe(instanceKey(wednesday))
    expect(sameInstance(monday, wednesday)).toBe(false)
    expect(sameInstance(monday, instance(1, '2026-08-31'))).toBe(true)
  })

  it('id 가 다르면 다른 회차다', () => {
    expect(instanceKey(instance(1, '2026-08-31'))).not.toBe(
      instanceKey(instance(2, '2026-08-31')),
    )
  })
})

describe('회차 하나만 갈아끼우기', () => {
  it('같은 id 의 다른 회차는 건드리지 않는다', () => {
    const list = [
      instance(1, '2026-08-31'),
      instance(1, '2026-09-02'),
      instance(1, '2026-09-04'),
    ]

    const next = replaceInstance(list, instance(1, '2026-09-02', { status: 'DONE' }))

    expect(next.map((o) => o.status)).toEqual(['PLANNED', 'DONE', 'PLANNED'])
  })

  it('없는 회차를 넣어도 목록이 늘지 않는다', () => {
    const list = [instance(1, '2026-08-31')]

    expect(replaceInstance(list, instance(9, '2026-08-31'))).toEqual(list)
  })
})

describe('달력 칸에 담기', () => {
  it('원래 날짜가 아니라 실제 시작일로 묶는다', () => {
    // 8/31 예정이던 회차를 9/1 로 옮겼다. 칸은 9/1 에 놓여야 한다
    const moved = instance(1, '2026-08-31', {
      startAt: '2026-09-01T07:00:00',
    })

    const grouped = groupByDate([moved])

    expect(grouped.get('2026-09-01')).toHaveLength(1)
    expect(grouped.has('2026-08-31')).toBe(false)
  })

  it('같은 날의 회차를 한 칸에 모은다', () => {
    const grouped = groupByDate([
      instance(1, '2026-08-31'),
      instance(2, '2026-08-31'),
      instance(3, '2026-09-01'),
    ])

    expect(grouped.get('2026-08-31')).toHaveLength(2)
    expect(grouped.get('2026-09-01')).toHaveLength(1)
  })

  it('여러 날에 걸친 일정은 걸친 날마다 놓는다', () => {
    const trip = instance(1, '2026-08-31', {
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
      instance(1, '2026-08-31', {
        startAt: '2026-08-31T09:00:00',
        endAt: '2026-08-31T18:00:00',
      }),
    ])

    expect(grouped.size).toBe(1)
  })

  it('종료가 시작보다 앞서도 시작일에서 사라지지 않는다', () => {
    const broken = instance(1, '2026-08-31', {
      startAt: '2026-08-31T09:00:00',
      endAt: '2026-08-30T18:00:00',
    })

    expect(groupByDate([broken]).get('2026-08-31')).toHaveLength(1)
  })
})

describe('그 날짜에 걸쳐 있는지', () => {
  const trip = instance(1, '2026-08-31', {
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
    const single = instance(2, '2026-08-31')

    expect(coversDate(single, new Date(2026, 7, 31))).toBe(true)
    expect(coversDate(single, new Date(2026, 8, 1))).toBe(false)
  })

  it('연기된 회차는 원래 날짜가 아니라 옮겨간 날에서 잡힌다', () => {
    const postponed = instance(3, '2026-08-31', {
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
    const text = formatInstanceRange(
      instance(1, '2026-08-31', {
        startAt: '2026-08-31T09:00:00',
        endAt: '2026-08-31T18:30:00',
      }),
    )

    expect(text).toBe('8/31 09:00 ~ 18:30')
  })

  it('날짜가 넘어가면 종료일자도 적는다', () => {
    const text = formatInstanceRange(
      instance(1, '2026-08-31', {
        startAt: '2026-08-31T09:00:00',
        endAt: '2026-09-02T18:00:00',
      }),
    )

    expect(text).toBe('8/31 09:00 ~ 9/2 18:00')
  })

  it('종료가 없으면 시작만 적는다', () => {
    expect(formatInstanceRange(instance(1, '2026-08-31'))).toBe('8/31 07:00')
  })

  it('하루 종일이면 시각을 적지 않는다', () => {
    const oneDay = instance(1, '2026-08-31', {
      allDay: true,
      startAt: '2026-08-31T00:00:00',
    })
    const span = instance(2, '2026-08-31', {
      allDay: true,
      startAt: '2026-08-31T00:00:00',
      endAt: '2026-09-02T23:59:00',
    })

    expect(formatInstanceRange(oneDay)).toBe('8/31')
    expect(formatInstanceRange(span)).toBe('8/31 ~ 9/2')
  })
})

describe('옮긴 회차', () => {
  it('원래 날짜와 시작일이 다르면 옮긴 것이다', () => {
    expect(isMoved(instance(1, '2026-08-31'))).toBe(false)
    expect(
      isMoved(instance(1, '2026-08-31', { startAt: '2026-09-01T07:00:00' })),
    ).toBe(true)
  })

  it('같은 날 안에서 시각만 바뀐 것은 옮긴 게 아니다', () => {
    expect(
      isMoved(instance(1, '2026-08-31', { startAt: '2026-08-31T20:00:00' })),
    ).toBe(false)
  })
})

describe('조기 종료', () => {
  const TODAY = '2026-08-31'

  it('아직 오지 않은 날을 완료하면 조기 종료다', () => {
    const future = instance(1, '2026-09-02', { status: 'DONE' })

    expect(isEarlyDone(future, TODAY)).toBe(true)
  })

  it('오늘 것을 완료한 건 조기 종료가 아니다', () => {
    const today = instance(1, TODAY, { status: 'DONE' })

    expect(isEarlyDone(today, TODAY)).toBe(false)
  })

  it('완료하지 않았으면 조기 종료가 아니다', () => {
    const future = instance(1, '2026-09-02')

    expect(isEarlyDone(future, TODAY)).toBe(false)
  })
})
