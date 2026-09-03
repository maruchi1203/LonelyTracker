import { describe, expect, it } from 'vitest'
import type { UserCategoryResponse, ScheduleResponse } from '../types/schedule'
import { applyFilters, countByCategory, matchesQuery, rankCategories } from './filter'

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
    postponeCount: 0,
    createdAt: '2026-08-01T00:00:00',
    updatedAt: '2026-08-01T00:00:00',
    ...overrides,
  }
}

function category(name: string, displayOrder: number): UserCategoryResponse {
  return {
    id: displayOrder,
    name,
    displayOrder,
    archived: false,
    createdAt: '2026-08-01T00:00:00',
    updatedAt: '2026-08-01T00:00:00',
  }
}

const LIST = [
  instance(1, { category: '육체' }),
  instance(2, { category: '육체' }),
  instance(3, { category: '능력' }),
  instance(4), // 분류 없음
]

describe('분류별 개수', () => {
  it('분류가 없는 일정은 세지 않는다', () => {
    const usage = countByCategory(LIST)

    expect(usage.get('육체')).toBe(2)
    expect(usage.get('능력')).toBe(1)
    expect(usage.size).toBe(2)
  })

  it('필터를 건 뒤가 아니라 창 전체로 세야 나머지 칩이 0이 되지 않는다', () => {
    const filtered = applyFilters(LIST, { category: '능력', query: '' })

    expect(countByCategory(filtered).get('육체')).toBeUndefined()
    expect(countByCategory(LIST).get('육체')).toBe(2)
  })
})

describe('분류 순위', () => {
  const categories = [category('육체', 0), category('능력', 1), category('취미', 2)]

  it('많이 쓴 순으로 앞에 온다', () => {
    expect(rankCategories(countByCategory(LIST), categories)[0]).toBe('육체')
  })

  it('개수가 같으면 순서가 매번 같다', () => {
    const usage = countByCategory([])
    const once = rankCategories(usage, categories)

    expect(once).toEqual(rankCategories(usage, categories))
    // 0건끼리는 displayOrder 로 갈린다
    expect(once).toEqual(['육체', '능력', '취미'])
  })

  it('목록에 없는 이름도 일정에 있으면 포함한다', () => {
    const usage = countByCategory([instance(9, { category: '즉흥' })])

    expect(rankCategories(usage, categories)).toContain('즉흥')
  })
})

describe('검색', () => {
  const item = instance(1, {
    title: 'Gym Workout',
    description: '스쿼트 5세트',
    category: '육체',
  })

  it('제목·내용·분류를 함께 본다', () => {
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
  it('분류와 검색어가 AND 로 걸린다', () => {
    const list = [
      instance(1, { category: '육체', title: '달리기' }),
      instance(2, { category: '육체', title: '독서' }),
      instance(3, { category: '능력', title: '달리기' }),
    ]

    expect(applyFilters(list, { category: '육체', query: '달리기' })).toHaveLength(1)
    expect(applyFilters(list, { category: null, query: '달리기' })).toHaveLength(2)
    expect(applyFilters(list, { category: '육체', query: '' })).toHaveLength(2)
  })
})
