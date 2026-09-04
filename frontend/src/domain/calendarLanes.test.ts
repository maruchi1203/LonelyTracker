import { describe, expect, it } from 'vitest'
import type { ScheduleResponse } from '../types/schedule'
import { buildMonthDays } from '../utils/monthGrid'
import { MAX_LANES, assignLanes } from './calendarLanes'

// 2026-08 그리드는 7/26(일) 부터 6주 42칸이다
const DAYS = buildMonthDays(new Date(2026, 7, 1))

function span(
  id: number,
  startDate: string,
  endDate?: string,
  overrides: Partial<ScheduleResponse> = {},
): ScheduleResponse {
  return {
    id,
    instanceDate: startDate,
    title: `일정${id}`,
    startAt: `${startDate}T09:00:00`,
    endAt: endDate ? `${endDate}T18:00:00` : undefined,
    allDay: false,
    status: 'PLANNED',
      createdAt: '2026-08-01T00:00:00',
    updatedAt: '2026-08-01T00:00:00',
    ...overrides,
  }
}

/** 그 날 그 일정이 놓인 레인 번호 */
function laneOf(
  map: ReturnType<typeof assignLanes>,
  key: string,
  id: number,
): number {
  return (map.get(key)?.lanes ?? []).findIndex((s) => s?.instance.id === id)
}

describe('레인 배정', () => {
  it('걸친 날마다 같은 레인에 놓는다', () => {
    // 이게 어긋나면 모서리를 붙여도 띠로 보이지 않는다
    const map = assignLanes(DAYS, [
      span(1, '2026-08-03', '2026-08-05'),
      span(2, '2026-08-04'),
    ])

    expect(laneOf(map, '2026-08-03', 1)).toBe(0)
    expect(laneOf(map, '2026-08-04', 1)).toBe(0)
    expect(laneOf(map, '2026-08-05', 1)).toBe(0)
    // 하루짜리는 밀려 아래 레인으로 간다
    expect(laneOf(map, '2026-08-04', 2)).toBe(1)
  })

  it('겹치지 않으면 같은 레인을 다시 쓴다', () => {
    const map = assignLanes(DAYS, [
      span(1, '2026-08-03', '2026-08-04'),
      span(2, '2026-08-06', '2026-08-07'),
    ])

    expect(laneOf(map, '2026-08-03', 1)).toBe(0)
    expect(laneOf(map, '2026-08-06', 2)).toBe(0)
  })

  it('시작과 끝을 표시한다', () => {
    const map = assignLanes(DAYS, [span(1, '2026-08-03', '2026-08-05')])
    const at = (key: string) => map.get(key)!.lanes[0]!

    expect(at('2026-08-03')).toMatchObject({ isStart: true, isEnd: false })
    expect(at('2026-08-04')).toMatchObject({ isStart: false, isEnd: false })
    expect(at('2026-08-05')).toMatchObject({ isStart: false, isEnd: true })
  })

  it('주가 바뀌면 띠를 끊는다', () => {
    // 8/8(토) ~ 8/10(월) — 토요일에서 한 주가 끝난다
    const map = assignLanes(DAYS, [span(1, '2026-08-08', '2026-08-10')])

    expect(map.get('2026-08-08')!.lanes[0]).toMatchObject({ isEnd: true })
    expect(map.get('2026-08-09')!.lanes[0]).toMatchObject({ isStart: true })
  })

  it('하루짜리는 시작이자 끝이다', () => {
    const map = assignLanes(DAYS, [span(1, '2026-08-03')])

    expect(map.get('2026-08-03')!.lanes[0]).toMatchObject({
      isStart: true,
      isEnd: true,
    })
  })

  it('레인이 모자라면 접어서 센다', () => {
    const many = Array.from({ length: MAX_LANES + 2 }, (_, i) =>
      span(i + 1, '2026-08-03'),
    )

    const day = assignLanes(DAYS, many).get('2026-08-03')!

    expect(day.lanes.filter(Boolean)).toHaveLength(MAX_LANES)
    expect(day.hidden).toBe(2)
  })

  it('빈 레인은 자리를 지킨다', () => {
    const map = assignLanes(DAYS, [
      span(1, '2026-08-03', '2026-08-05'),
      span(2, '2026-08-05'),
    ])

    // 8/5 는 레인 0 이 이어지는 띠, 레인 1 이 새 일정
    expect(map.get('2026-08-05')!.lanes).toHaveLength(2)
    expect(map.get('2026-08-04')!.lanes).toHaveLength(1)
  })

  it('같은 입력이면 매번 같은 결과다', () => {
    const input = [
      span(2, '2026-08-03'),
      span(1, '2026-08-03'),
      span(3, '2026-08-03'),
    ]

    const once = assignLanes(DAYS, input)
    const twice = assignLanes(DAYS, [...input].reverse())

    // 조회할 때마다 띠가 뛰면 안 된다
    expect(laneOf(once, '2026-08-03', 1)).toBe(laneOf(twice, '2026-08-03', 1))
    expect(laneOf(once, '2026-08-03', 3)).toBe(laneOf(twice, '2026-08-03', 3))
  })
})
