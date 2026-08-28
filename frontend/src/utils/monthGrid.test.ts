import { describe, expect, it } from 'vitest'
import { toLocalDate } from './datetime'
import { buildMonthDays, fromMonthParam, monthGridWindow, toMonthParam } from './monthGrid'

const AUGUST_2026 = new Date(2026, 7, 1)

describe('월 그리드', () => {
  it('주 단위로 채워 일요일에 시작한다', () => {
    const days = buildMonthDays(AUGUST_2026)

    // 2026-08-01 은 토요일이라 앞에 6칸이 붙어 6주가 된다
    expect(days).toHaveLength(42)
    expect(days.length % 7).toBe(0)
    expect(days[0].getDay()).toBe(0)
    expect(toLocalDate(days[0])).toBe('2026-07-26')
  })

  it('이번 달 1일과 말일을 모두 담는다', () => {
    const keys = buildMonthDays(AUGUST_2026).map(toLocalDate)

    expect(keys).toContain('2026-08-01')
    expect(keys).toContain('2026-08-31')
  })
})

describe('조회 기간', () => {
  it('첫 칸의 0시부터 마지막 칸의 하루 끝까지다', () => {
    const { from, to } = monthGridWindow(AUGUST_2026)
    const days = buildMonthDays(AUGUST_2026)

    expect(from).toBe(`${toLocalDate(days[0])}T00:00:00`)
    // 마지막 칸의 밤 일정이 빠지지 않으려면 하루 끝이어야 한다
    expect(to).toBe(`${toLocalDate(days[days.length - 1])}T23:59:59`)
  })

  it('그리드가 그리는 칸과 같은 범위를 준다', () => {
    const days = buildMonthDays(AUGUST_2026)
    const { from, to } = monthGridWindow(AUGUST_2026)

    expect(from.startsWith(toLocalDate(days[0]))).toBe(true)
    expect(to.startsWith(toLocalDate(days.at(-1)!))).toBe(true)
  })
})

describe('URL 파라미터', () => {
  it('달을 왕복해도 같은 값이다', () => {
    expect(toMonthParam(AUGUST_2026)).toBe('2026-08')
    expect(fromMonthParam('2026-08').getTime()).toBe(AUGUST_2026.getTime())
  })

  it('형식이 어긋나면 이번 달 1일로 돌아간다', () => {
    const now = new Date()

    for (const bad of [null, '', '2026', '2026-13-01', '2026-13', '2026-00', 'abc']) {
      const fallback = fromMonthParam(bad)
      expect(fallback.getFullYear()).toBe(now.getFullYear())
      expect(fallback.getMonth()).toBe(now.getMonth())
      expect(fallback.getDate()).toBe(1)
    }
  })
})
