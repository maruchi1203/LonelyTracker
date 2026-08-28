import { describe, expect, it } from 'vitest'
import type { ParsedSchedule } from '../types/parse'
import type { Draft } from './draft'
import { draftFromParsed, draftToCreateRequest, draftValidationError } from './draft'

const PARSED: ParsedSchedule = {
  title: '운동',
  startAt: '2026-08-31T07:00:00',
  allDay: false,
  place: '헬스장',
  recurrence: { freq: 'WEEKLY', byWeekday: ['MONDAY', 'WEDNESDAY', 'FRIDAY'] },
  questions: [],
}

function draft(overrides: Partial<Draft> = {}): Draft {
  return { ...draftFromParsed(PARSED, null), ...overrides }
}

describe('초안 만들기', () => {
  it('파싱 결과를 입력칸 형식으로 옮긴다', () => {
    const d = draftFromParsed(PARSED, null)

    expect(d.title).toBe('운동')
    expect(d.startAt).toBe('2026-08-31T07:00')
    expect(d.place).toBe('헬스장')
    expect(d.recurring).toBe(true)
    expect(d.byWeekday).toEqual(['MONDAY', 'WEDNESDAY', 'FRIDAY'])
  })

  it('시작 시각을 못 채웠어도 비워두지 않는다', () => {
    const d = draftFromParsed({ title: '회의', allDay: false }, new Date(2026, 7, 31))

    expect(d.startAt).toMatch(/^2026-08-31T\d{2}:\d{2}$/)
    expect(d.recurring).toBe(false)
  })
})

describe('저장 전 검증', () => {
  it('매주 반복인데 요일이 없으면 막는다', () => {
    expect(draftValidationError(draft({ byWeekday: [] }))).not.toBeNull()
  })

  it('매일 반복은 요일이 없어도 된다', () => {
    expect(draftValidationError(draft({ freq: 'DAILY', byWeekday: [] }))).toBeNull()
  })

  it('제목과 시작 시각은 있어야 한다', () => {
    expect(draftValidationError(draft({ title: '  ' }))).not.toBeNull()
    expect(draftValidationError(draft({ startAt: '' }))).not.toBeNull()
  })
})

describe('생성 요청으로 바꾸기', () => {
  it('초를 붙여 백엔드 LocalDateTime 에 맞춘다', () => {
    expect(draftToCreateRequest(draft()).startAt).toBe('2026-08-31T07:00:00')
  })

  it('반복이 꺼져 있으면 recurrence 를 보내지 않는다', () => {
    expect(draftToCreateRequest(draft({ recurring: false })).recurrence).toBeUndefined()
  })

  it('매일 반복이면 요일을 보내지 않는다', () => {
    const body = draftToCreateRequest(draft({ freq: 'DAILY' }))

    expect(body.recurrence?.freq).toBe('DAILY')
    expect(body.recurrence?.byWeekday).toBeUndefined()
  })

  it('장소는 체크했을 때만 메모로 넘어간다', () => {
    expect(draftToCreateRequest(draft()).description).toBeUndefined()
    expect(
      draftToCreateRequest(draft({ keepPlaceInDescription: true })).description,
    ).toBe('장소: 헬스장')
  })

  it('빈 칸은 undefined 로 보내 서버가 기본값을 쓰게 한다', () => {
    const body = draftToCreateRequest(draft({ category: '  ', endAt: '', endsOn: '' }))

    expect(body.category).toBeUndefined()
    expect(body.endAt).toBeUndefined()
    expect(body.recurrence?.endsOn).toBeUndefined()
  })
})
