import type { UserCategoryResponse, ScheduleResponse } from '../types/schedule'

/** 칩에 보여줄 분류 개수 */
export const TOP_CATEGORIES = 5

/**
 * 분류별 사용 횟수. 백엔드에 집계가 없어 불러온 회차에서 센다.
 * 필터를 걸기 전의 창 전체로 세야 칩 하나를 골랐을 때 나머지가 0이 되지 않는다.
 */
export function countByCategory(
  instances: ScheduleResponse[],
): Map<string, number> {
  const counts = new Map<string, number>()

  for (const instance of instances) {
    const name = instance.category
    if (!name) continue
    counts.set(name, (counts.get(name) ?? 0) + 1)
  }

  return counts
}

/**
 * 많이 쓴 순으로 정렬한 분류 이름.
 * 동점은 displayOrder, 이름순으로 갈라 조회할 때마다 칩이 뒤섞이지 않게 한다.
 */
export function rankCategories(
  usage: Map<string, number>,
  categories: UserCategoryResponse[],
): string[] {
  const order = new Map(categories.map((c) => [c.name, c.displayOrder]))

  // 목록에 없는 이름도 일정에는 들어갈 수 있으므로 둘을 합친다
  const names = new Set<string>([
    ...categories.filter((c) => !c.archived).map((c) => c.name),
    ...usage.keys(),
  ])

  return [...names].sort((a, b) => {
    const byCount = (usage.get(b) ?? 0) - (usage.get(a) ?? 0)
    if (byCount !== 0) return byCount

    const byOrder = (order.get(a) ?? Number.MAX_SAFE_INTEGER) - (order.get(b) ?? Number.MAX_SAFE_INTEGER)
    if (byOrder !== 0) return byOrder

    return a.localeCompare(b)
  })
}

/** 제목·내용·분류에 검색어가 들어 있는지 */
export function matchesQuery(instance: ScheduleResponse, query: string): boolean {
  const needle = query.trim().toLocaleLowerCase()
  if (!needle) return true

  return [instance.title, instance.description ?? '', instance.category ?? '']
    .some((field) => field.toLocaleLowerCase().includes(needle))
}

/** 분류와 검색어를 함께 건다. 날짜 선택은 여기 넣지 않는다 — 목록에만 적용된다 */
export function applyFilters(
  instances: ScheduleResponse[],
  filters: { category: string | null; query: string },
): ScheduleResponse[] {
  return instances
    .filter((o) => !filters.category || o.category === filters.category)
    .filter((o) => matchesQuery(o, filters.query))
}
