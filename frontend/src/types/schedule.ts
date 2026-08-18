// 백엔드 ScheduleResponse / 요청 DTO와 짝을 맞춘 타입.
// 필드명을 틀리면 컴파일 단계에서 잡힌다.

export type ScheduleStatus = 'PLANNED' | 'DONE' | 'SKIPPED'

/** 카테고리 계층 구분자. 예: 능력\개발\SpringBoot */
export const CATEGORY_SEPARATOR = '\\'

export interface Schedule {
  id: number
  title: string
  /** 마크다운 원문 */
  description?: string
  startAt: string
  endAt?: string
  allDay: boolean
  status: ScheduleStatus
  category?: string
  createdAt: string
  updatedAt: string
}

export interface ScheduleCreateRequest {
  title: string
  description?: string
  startAt: string
  endAt?: string
  allDay?: boolean
  category?: string
}

/** 목록 조회 조건. 준 것만 AND로 걸린다. */
export interface ScheduleQuery {
  from?: string
  to?: string
  status?: ScheduleStatus
  /** 하위 카테고리까지 포함해 조회된다 */
  category?: string
}

/** 백엔드 GlobalExceptionHandler가 내려주는 에러 형태 */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

/** "능력\개발\SpringBoot" → ["능력", "개발", "SpringBoot"] */
export function categorySegments(category?: string): string[] {
  if (!category) return []
  return category.split(CATEGORY_SEPARATOR).filter(Boolean)
}

/** 목록에 등장하는 모든 카테고리와 그 조상 경로를 중복 없이 모은다 */
export function collectCategories(schedules: Schedule[]): string[] {
  const set = new Set<string>()
  for (const s of schedules) {
    const segments = categorySegments(s.category)
    for (let i = 0; i < segments.length; i++) {
      set.add(segments.slice(0, i + 1).join(CATEGORY_SEPARATOR))
    }
  }
  return [...set].sort((a, b) => a.localeCompare(b))
}
