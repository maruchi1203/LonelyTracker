import type { ScheduleResponse } from '../types/schedule'

/** 칩에 보여줄 태그 개수 */
export const TOP_TAGS = 5

/**
 * 태그별 사용 횟수. 백엔드에 집계가 없어 불러온 회차에서 센다.
 * 필터를 걸기 전의 창 전체로 세야 칩 하나를 골랐을 때 나머지가 0이 되지 않는다.
 * 태그가 여러 개면 그 일정은 각 태그에 한 번씩 센다.
 */
export function countByTag(instances: ScheduleResponse[]): Map<string, number> {
  const counts = new Map<string, number>()

  for (const instance of instances) {
    for (const name of instance.tags ?? []) {
      counts.set(name, (counts.get(name) ?? 0) + 1)
    }
  }

  return counts
}

/**
 * 많이 쓴 순으로 정렬한 태그 이름.
 * 동점은 이름순으로 갈라 조회할 때마다 칩이 뒤섞이지 않게 한다.
 *
 * @param known 이미 쓴 적 있는 태그. 이번 창에 안 나온 것도 후보로 남긴다
 */
export function rankTags(
  usage: Map<string, number>,
  known: string[] = [],
): string[] {
  const names = new Set<string>([...known, ...usage.keys()])

  return [...names].sort((a, b) => {
    const byCount = (usage.get(b) ?? 0) - (usage.get(a) ?? 0)
    if (byCount !== 0) return byCount

    return a.localeCompare(b)
  })
}

/** 제목·내용·태그에 검색어가 들어 있는지 */
export function matchesQuery(instance: ScheduleResponse, query: string): boolean {
  const needle = query.trim().toLocaleLowerCase()
  if (!needle) return true

  return [instance.title, instance.description ?? '', ...(instance.tags ?? [])]
    .some((field) => field.toLocaleLowerCase().includes(needle))
}

/** 태그와 검색어를 함께 건다. 날짜 선택은 여기 넣지 않는다 — 목록에만 적용된다 */
export function applyFilters(
  instances: ScheduleResponse[],
  filters: { tag: string | null; query: string },
): ScheduleResponse[] {
  return instances
    .filter((o) => !filters.tag || (o.tags ?? []).includes(filters.tag))
    .filter((o) => matchesQuery(o, filters.query))
}
