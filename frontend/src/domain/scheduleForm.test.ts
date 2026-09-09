import { describe, expect, it } from 'vitest'
import type { ParsedSchedule } from '../types/parse'
import type { ScheduleDetailResponse, Weekday } from '../types/schedule'
import type { ScheduleForm } from './scheduleForm'
import {
  draftFromParsed,
  emptyForm,
  formFromDetail,
  formToCreateRequest,
  formValidationError,
} from './scheduleForm'

const PARSED: ParsedSchedule = {
  title: '운동',
  startAt: '2026-08-31T07:00:00',
  allDay: false,
  place: '헬스장',
  recurrence: { freq: 'WEEKLY', byWeekday: ['MONDAY', 'WEDNESDAY', 'FRIDAY'] },
  questions: [],
}

const WMF: Weekday[] = ['MONDAY', 'WEDNESDAY', 'FRIDAY']

function form(overrides: Partial<ScheduleForm> = {}): ScheduleForm {
  return {
    ...emptyForm(new Date(2026, 7, 31)),
    title: '러닝',
    startTime: '07:00',
    ...overrides,
  }
}

describe('초안 만들기', () => {
  it('날짜와 시각을 따로 나눠 담는다', () => {
    const d = draftFromParsed(PARSED, null)

    expect(d.startDate).toBe('2026-08-31')
    expect(d.startTime).toBe('07:00')
    expect(d.repeating).toBe(true)
    expect(d.byWeekday).toEqual(['MONDAY', 'WEDNESDAY', 'FRIDAY'])
    expect(d.place).toBe('헬스장')
  })

  it('반복이면 종료일자 칸이 반복 종료일을 받는다', () => {
    const d = draftFromParsed(
      { ...PARSED, recurrence: { freq: 'DAILY', endsOn: '2026-09-02' } },
      null,
    )

    expect(d.endDate).toBe('2026-09-02')
  })

  it('하루 종일이면 시작시각을 비운다', () => {
    const d = draftFromParsed({ ...PARSED, allDay: true }, null)

    expect(d.startTime).toBe('')
  })

  it('달력은 시작일을 못 채웠어도 비워두지 않는다', () => {
    const d = draftFromParsed({ title: '회의', allDay: false }, new Date(2026, 7, 31))

    expect(d.startDate).toBe('2026-08-31')
    expect(d.repeating).toBe(false)
  })

  it('리스트는 못 채운 시작일을 비워 둔다', () => {
    // "언젠가 책 읽기" 가 오늘 일정이 되면 안 된다
    const d = draftFromParsed(
      { title: '책 읽기', allDay: false },
      new Date(2026, 7, 31),
      'list',
    )

    expect(d.startDate).toBe('')
    expect(d.startTime).toBe('')
  })

  it('리스트라도 AI 가 날짜를 뽑았으면 그대로 쓴다', () => {
    const d = draftFromParsed(
      { title: '회의', startAt: '2026-10-01T14:00:00', allDay: false },
      null,
      'list',
    )

    expect(d.startDate).toBe('2026-10-01')
    expect(d.startTime).toBe('14:00')
  })
})

describe('저장 전 검증', () => {
  it('제목과 시작일자는 있어야 한다', () => {
    expect(formValidationError(form({ title: '  ' }))).not.toBeNull()
    expect(formValidationError(form({ startDate: '' }))).not.toBeNull()
    expect(formValidationError(form())).toBeNull()
  })

  it('매주 반복인데 요일이 없으면 막는다', () => {
    const weekly = form({ repeating: true, freq: 'WEEKLY', byWeekday: [] })

    expect(formValidationError(weekly)).not.toBeNull()
    expect(formValidationError({ ...weekly, byWeekday: ['MONDAY'] })).toBeNull()
  })

  it('매일 반복은 요일이 없어도 된다', () => {
    expect(formValidationError(form({ repeating: true, freq: 'DAILY' }))).toBeNull()
  })

  it('매월은 아직 보낼 수 없다고 알린다', () => {
    expect(formValidationError(form({ repeating: true, freq: 'MONTHLY' }))).toContain(
      '준비 중',
    )
  })

  it('종료가 시작보다 앞서면 막는다', () => {
    expect(formValidationError(form({ endDate: '2026-08-30' }))).not.toBeNull()
    expect(formValidationError(form({ endTime: '06:00' }))).not.toBeNull()
    expect(formValidationError(form({ endTime: '08:00' }))).toBeNull()
  })
})

describe('반복 소요시간', () => {
  it('매일 반복이 24시간을 넘으면 막는다', () => {
    const error = formValidationError(
      form({ repeating: true, freq: 'DAILY', durationHours: '25' }),
    )

    expect(error).toContain('최대 24시간')
  })

  it('매주 월수금은 48시간까지 허용한다', () => {
    const base = { repeating: true, freq: 'WEEKLY' as const, byWeekday: WMF }

    expect(formValidationError(form({ ...base, durationHours: '48' }))).toBeNull()
    expect(formValidationError(form({ ...base, durationHours: '49' }))).toContain(
      '최대 48시간',
    )
  })

  it('매주 한 요일만 고르면 일주일까지 허용한다', () => {
    const error = formValidationError(
      form({
        repeating: true,
        freq: 'WEEKLY',
        byWeekday: ['MONDAY'],
        durationHours: '167',
      }),
    )

    expect(error).toBeNull()
  })
})

describe('생성 요청으로 바꾸기', () => {
  it('날짜와 시각을 합쳐 LocalDateTime 으로 만든다', () => {
    expect(formToCreateRequest(form()).startAt).toBe('2026-08-31T07:00:00')
  })

  it('시작시각을 비우면 하루 종일로 보낸다', () => {
    const body = formToCreateRequest(form({ startTime: '' }))

    expect(body.allDay).toBe(true)
    expect(body.startAt).toBe('2026-08-31T00:00:00')
  })

  it('한번만이면 종료일자가 endAt 으로 간다', () => {
    const body = formToCreateRequest(form({ endDate: '2026-09-02', endTime: '09:00' }))

    expect(body.endAt).toBe('2026-09-02T09:00:00')
    expect(body.recurrence).toBeUndefined()
  })

  it('반복이면 종료일자는 endsOn 으로, 소요시간은 endAt 으로 간다', () => {
    // 회차마다 날짜가 달라 절대 종료시각을 쓸 수 없다
    const body = formToCreateRequest(
      form({
        repeating: true,
        freq: 'WEEKLY',
        byWeekday: ['MONDAY'],
        endDate: '2026-09-30',
        durationHours: '1',
        durationMins: '0',
      }),
    )

    expect(body.recurrence?.endsOn).toBe('2026-09-30')
    expect(body.endAt).toBe('2026-08-31T08:00:00')
  })

  it('자정을 넘는 반복은 endAt 이 다음 날로 간다', () => {
    const body = formToCreateRequest(
      form({
        repeating: true,
        freq: 'DAILY',
        startTime: '22:00',
        durationHours: '4',
      }),
    )

    expect(body.endAt).toBe('2026-09-01T02:00:00')
  })

  it('소요시간을 비우면 endAt 을 보내지 않는다', () => {
    const body = formToCreateRequest(form({ repeating: true, freq: 'DAILY' }))

    expect(body.endAt).toBeUndefined()
  })

  it('매일 반복은 요일을 싣지 않는다', () => {
    const body = formToCreateRequest(form({ repeating: true, freq: 'DAILY' }))

    expect(body.recurrence?.freq).toBe('DAILY')
    expect(body.recurrence?.byWeekday).toBeUndefined()
  })

  it('빈 칸은 undefined 로 보내 서버가 기본값을 쓰게 한다', () => {
    const body = formToCreateRequest(form({ tags: [] }))

    expect(body.tags).toBeUndefined()
    expect(body.endAt).toBeUndefined()
  })

  it('태그는 여러 개를 그대로 보낸다', () => {
    const body = formToCreateRequest(form({ tags: ['육체', '아침'] }))

    expect(body.tags).toEqual(['육체', '아침'])
  })

  it('장소와 2분 행동을 그대로 보낸다. 메모를 거치지 않는다', () => {
    const body = formToCreateRequest(
      form({ place: '헬스장', twoMinuteAction: '운동복 갈아입기' }),
    )

    expect(body.place).toBe('헬스장')
    expect(body.twoMinuteAction).toBe('운동복 갈아입기')
    expect(body.description).toBeUndefined()
  })

  it('장소와 2분 행동이 비면 아예 보내지 않는다', () => {
    const body = formToCreateRequest(form({ place: '  ', twoMinuteAction: '' }))

    expect(body.place).toBeUndefined()
    expect(body.twoMinuteAction).toBeUndefined()
  })
})

describe('날짜 없는 리스트 항목', () => {
  it('리스트는 시작일자 없이 통과한다', () => {
    expect(formValidationError(form({ startDate: '' }), 'list')).toBeNull()
  })

  it('달력은 시작일자를 계속 요구한다', () => {
    expect(formValidationError(form({ startDate: '' }), 'calendar')).toMatch(
      '시작일자',
    )
  })

  it('변형을 안 주면 달력으로 본다', () => {
    expect(formValidationError(form({ startDate: '' }))).toMatch('시작일자')
  })

  it('리스트라도 반복이면 시작일자가 필요하다', () => {
    const f = form({ startDate: '', repeating: true, freq: 'DAILY' })

    expect(formValidationError(f, 'list')).toMatch('시작일자')
  })

  it('날짜를 비우면 startAt 도 endAt 도 보내지 않는다', () => {
    const body = formToCreateRequest(form({ startDate: '', endDate: '2026-09-01' }))

    expect(body.startAt).toBeUndefined()
    expect(body.endAt).toBeUndefined()
    expect(body.allDay).toBe(false)
  })

  it('기한과 상위를 그대로 보낸다', () => {
    const body = formToCreateRequest(
      form({ startDate: '', dueOn: '2026-10-01', parentId: '7' }),
    )

    expect(body.dueOn).toBe('2026-10-01')
    expect(body.parentId).toBe(7)
  })

  it('기한과 상위가 비면 아예 보내지 않는다', () => {
    const body = formToCreateRequest(form())

    expect(body.dueOn).toBeUndefined()
    expect(body.parentId).toBeUndefined()
  })
})

function detail(
  overrides: Partial<ScheduleDetailResponse> = {},
): ScheduleDetailResponse {
  return {
    id: 1,
    title: '보고서',
    allDay: false,
    createdAt: '2026-09-09T00:00:00',
    updatedAt: '2026-09-09T00:00:00',
    ...overrides,
  }
}

describe('수정 폼으로 되돌리기', () => {
  it('시작일시를 날짜와 시각으로 나눈다', () => {
    const f = formFromDetail(
      detail({ startAt: '2026-10-01T09:00:00', endAt: '2026-10-01T10:30:00' }),
    )

    expect(f.startDate).toBe('2026-10-01')
    expect(f.startTime).toBe('09:00')
    expect(f.endTime).toBe('10:30')
    expect(f.repeating).toBe(false)
  })

  it('날짜가 없으면 빈 칸으로 둔다', () => {
    // emptyForm 이 채우는 오늘 날짜가 남으면 안 된다
    const f = formFromDetail(detail())

    expect(f.startDate).toBe('')
    expect(f.startTime).toBe('')
  })

  it('기한과 상위를 문자열로 받는다', () => {
    const f = formFromDetail(detail({ dueOn: '2026-10-05', parentId: 7 }))

    expect(f.dueOn).toBe('2026-10-05')
    expect(f.parentId).toBe('7')
  })

  it('상위가 없으면 빈 문자열이다', () => {
    expect(formFromDetail(detail()).parentId).toBe('')
  })

  it('반복이면 종료일자 칸이 반복 종료일을 받는다', () => {
    const f = formFromDetail(
      detail({
        startAt: '2026-10-01T07:00:00',
        endAt: '2026-10-01T08:30:00',
        recurrence: { freq: 'WEEKLY', byWeekday: WMF, endsOn: '2026-12-31' },
      }),
    )

    expect(f.repeating).toBe(true)
    expect(f.endDate).toBe('2026-12-31')
    expect(f.durationHours).toBe('1')
    expect(f.durationMins).toBe('30')
    expect(f.byWeekday).toEqual(WMF)
  })

  it('하루 종일이면 시작시각을 비운다', () => {
    const f = formFromDetail(
      detail({ startAt: '2026-10-01T00:00:00', allDay: true }),
    )

    expect(f.startTime).toBe('')
  })

  it('읽어서 그대로 다시 보내면 값이 살아남는다', () => {
    const source = detail({
      startAt: '2026-10-01T09:00:00',
      endAt: '2026-10-01T10:00:00',
      tags: ['업무'],
      place: '사무실',
      dueOn: '2026-10-05',
      parentId: 7,
    })

    const body = formToCreateRequest(formFromDetail(source))

    expect(body.startAt).toBe('2026-10-01T09:00:00')
    expect(body.dueOn).toBe('2026-10-05')
    expect(body.parentId).toBe(7)
    expect(body.tags).toEqual(['업무'])
    expect(body.place).toBe('사무실')
  })
})

describe('반복 일정을 리스트 얼굴로 수정할 때', () => {
  const recurring = detail({
    startAt: '2026-10-01T07:00:00',
    endAt: '2026-10-01T08:00:00',
    recurrence: { freq: 'WEEKLY', byWeekday: WMF, endsOn: '2026-12-31' },
  })

  it('반복 규칙이 살아남는다', () => {
    // 반복 토글이 안 보이는 얼굴로 고쳐도 규칙이 지워지면 안 된다
    const body = formToCreateRequest(formFromDetail(recurring))

    expect(body.recurrence).toEqual({
      freq: 'WEEKLY',
      byWeekday: WMF,
      endsOn: '2026-12-31',
    })
  })

  it('소요시간이 endAt 으로 돌아간다', () => {
    const body = formToCreateRequest(formFromDetail(recurring))

    expect(body.endAt).toBe('2026-10-01T08:00:00')
  })
})
