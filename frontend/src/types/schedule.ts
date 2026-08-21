// 백엔드 DTO와 짝을 맞춘 타입. 필드명을 틀리면 컴파일 단계에서 잡힌다.

export type ScheduleStatus = 'PLANNED' | 'DONE' | 'SKIPPED'

/** 사용자가 고를 수 있는 카테고리 목록의 한 항목 (GET /api/categories) */
export interface Category {
  id: number;
  name: string;
  color?: string;
  displayOrder: number;
  archived: boolean;
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
  /** 카테고리 이름을 문자열로 기록한다. 목록에 없는 이름도 들어갈 수 있다 */
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
  /** 이름이 정확히 일치하는 일정만 */
  category?: string
}

export interface User {
  id: number
  username: string
  displayName?: string
  createdAt: string
}

/** 백엔드 GlobalExceptionHandler가 내려주는 에러 형태 */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
