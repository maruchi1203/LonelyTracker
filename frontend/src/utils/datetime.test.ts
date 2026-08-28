import { describe, expect, it } from 'vitest'
import {
  formatTime,
  isSameDay,
  toLocalDate,
  toLocalDateTime,
  toLocalInputValue,
} from './datetime'

describe('지역 시각 변환', () => {
  it('타임존 표기 없이 백엔드 LocalDateTime 형식으로 만든다', () => {
    const at = new Date(2026, 7, 28, 14, 30, 5)

    expect(toLocalDateTime(at)).toBe('2026-08-28T14:30:05')
    expect(toLocalInputValue(at)).toBe('2026-08-28T14:30')
    expect(toLocalDate(at)).toBe('2026-08-28')
  })

  it('밤 시각이어도 날짜가 밀리지 않는다', () => {
    // toISOString() 을 쓰면 UTC 로 당겨져 하루 어긋난다
    const lateNight = new Date(2026, 7, 28, 23, 59, 0)

    expect(toLocalDate(lateNight)).toBe('2026-08-28')
    expect(toLocalDateTime(lateNight)).not.toContain('Z')
  })

  it('한 자리 수를 0으로 채운다', () => {
    expect(toLocalDateTime(new Date(2026, 0, 2, 3, 4, 5))).toBe('2026-01-02T03:04:05')
    expect(formatTime(new Date(2026, 0, 2, 3, 4))).toBe('03:04')
  })

  it('시·분이 달라도 같은 날이면 같다고 본다', () => {
    expect(isSameDay(new Date(2026, 7, 28, 0, 0), new Date(2026, 7, 28, 23, 59))).toBe(true)
    expect(isSameDay(new Date(2026, 7, 28), new Date(2026, 7, 29))).toBe(false)
    expect(isSameDay(new Date(2026, 7, 28), new Date(2025, 7, 28))).toBe(false)
  })
})
