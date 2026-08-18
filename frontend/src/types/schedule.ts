// 백엔드 DTO와 짝을 맞춘 타입. 필드명을 틀리면 컴파일 단계에서 잡힌다.

export type ScheduleStatus = 'PLANNED' | 'DONE' | 'SKIPPED'

/** 카테고리 계층 구분자. 예: 능력\개발\SpringBoot */
export const CATEGORY_SEPARATOR = '\\'

/** 사이드바용 전체 정보 (GET /api/categories) */
export interface Category {
  id: number
  name: string
  path: string
  parentId: number | null
  depth: number
  displayOrder: number
  color?: string
  collapsed: boolean
  archived: boolean
  createdAt: string
  updatedAt: string
}

/** 일정에 붙어 오는 축약형 */
export interface CategorySummary {
  id: number
  name: string
  path: string
  color?: string
}

export interface Schedule {
  id: number
  title: string
  /** 마크다운 원문 */
  description?: string
  startAt: string
  endAt?: string
  allDay: boolean
  status: ScheduleStatus
  category?: CategorySummary
  createdAt: string
  updatedAt: string
}

export interface ScheduleCreateRequest {
  title: string
  description?: string
  startAt: string
  endAt?: string
  allDay?: boolean
  /** 없는 경로면 중간 단계까지 서버가 함께 만든다 */
  categoryPath?: string
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
