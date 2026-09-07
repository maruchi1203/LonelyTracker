import { describe, expect, it } from 'vitest'
import type { ScheduleResponse } from '../types/schedule'
import { applyFilters, countByTag, matchesQuery, rankTags } from './filter'

function instance(
  id: number,
  overrides: Partial<ScheduleResponse> = {},
): ScheduleResponse {
  return {
    id,
    instanceDate: '2026-08-31',
    title: '운동',
    startAt: '2026-08-31T07:00:00',
    allDay: false,
    status: 'PLANNED',
    createdAt: '2026-08-01T00:00:00',
    updatedAt: '2026-08-01T00:00:00',
    ...overrides,
  }
}

const LIST = [
  instance(1, { tags: ['육체'] }),
  instance(2, { tags: ['육체'] }),
  instance(3, { tags: ['능력'] }),
  instance(4), // 태그 없음
]

describe('태그별 개수', () => {
  it('태그가 없는 일정은 세지 않는다', () => {
    const usage = countByTag(LIST)

    expect(usage.get('육체')).toBe(2)
    expect(usage.get('능력')).toBe(1)
    expect(usage.size).toBe(2)
  })

  it('태그가 여러 개면 각각에 한 번씩 센다', () => {
    const usage = countByTag([instance(9, { tags: ['육체', '아침'] })])

    expect(usage.get('육체')).toBe(1)
    expect(usage.get('아침')).toBe(1)
  })

  it('필터를 건 뒤가 아니라 창 전체로 세야 나머지 칩이 0이 되지 않는다', () => {
    const filtered = applyFilters(LIST, { tag: '능력', query: '' })

    expect(countByTag(filtered).get('육체')).toBeUndefined()
    expect(countByTag(LIST).get('육체')).toBe(2)
  })
})

describe('태그 순위', () => {
  const known = ['육체', '능력', '취미']

  it('많이 쓴 순으로 앞에 온다', () => {
    expect(rankTags(countByTag(LIST), known)[0]).toBe('육체')
  })

  it('개수가 같으면 순서가 매번 같다', () => {
    const usage = countByTag([])
    const once = rankTags(usage, known)

    expect(once).toEqual(rankTags(usage, known))
    // 0건끼리는 이름순으로 갈린다
    expect(once).toEqual(['능력', '육체', '취미'])
  })

  it('이번 창에만 있는 태그도 포함한다', () => {
    const usage = countByTag([instance(9, { tags: ['즉흥'] })])

    expect(rankTags(usage, known)).toContain('즉흥')
  })

  it('후보를 안 줘도 쓰인 태그만으로 순위가 나온다', () => {
    expect(rankTags(countByTag(LIST))).toEqual(['육체', '능력'])
  })
})

describe('검색', () => {
  const item = instance(1, {
    title: 'Gym Workout',
    description: '스쿼트 5세트',
    tags: ['육체'],
  })

  it('제목·내용·태그를 함께 본다', () => {
    expect(matchesQuery(item, '스쿼트')).toBe(true)
    expect(matchesQuery(item, '육체')).toBe(true)
    expect(matchesQuery(item, 'workout')).toBe(true)
  })

  it('대소문자를 가리지 않고 빈 검색어는 모두 통과시킨다', () => {
    expect(matchesQuery(item, 'GYM')).toBe(true)
    expect(matchesQuery(item, '   ')).toBe(true)
    expect(matchesQuery(item, '독서')).toBe(false)
  })
})

describe('필터 합성', () => {
  it('태그와 검색어가 AND 로 걸린다', () => {
    const list = [
      instance(1, { tags: ['육체'], title: '달리기' }),
      instance(2, { tags: ['육체'], title: '독서' }),
      instance(3, { tags: ['능력'], title: '달리기' }),
    ]

    expect(applyFilters(list, { tag: '육체', query: '달리기' })).toHaveLength(1)
    expect(applyFilters(list, { tag: null, query: '달리기' })).toHaveLength(2)
    expect(applyFilters(list, { tag: '육체', query: '' })).toHaveLength(2)
  })

  it('태그를 여러 개 가진 일정은 그중 아무 태그로도 걸린다', () => {
    const list = [instance(1, { tags: ['육체', '아침'] })]

    expect(applyFilters(list, { tag: '육체', query: '' })).toHaveLength(1)
    expect(applyFilters(list, { tag: '아침', query: '' })).toHaveLength(1)
    expect(applyFilters(list, { tag: '없는태그', query: '' })).toHaveLength(0)
  })
})
