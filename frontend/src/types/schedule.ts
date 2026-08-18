// 백엔드 ScheduleResponse / 요청 DTO와 짝을 맞춘 타입.
// 필드명을 틀리면 컴파일 단계에서 잡힌다.

export type ScheduleStatus = 'PLANNED' | 'DONE' | 'SKIPPED'
export type ScheduleSource = 'MANUAL' | 'AI_PARSED'

export interface Schedule {
  id: number
  title: string
  description?: string
  startAt: string
  endAt?: string
  allDay: boolean
  status: ScheduleStatus
  category?: string
  source: ScheduleSource
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

/** 백엔드 GlobalExceptionHandler가 내려주는 에러 형태 */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
