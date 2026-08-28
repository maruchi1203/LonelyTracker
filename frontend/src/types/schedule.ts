// 백엔드 DTO와 짝을 맞춘 타입. 이름도 백엔드를 따른다.
// 필드명을 틀리면 컴파일 단계에서 잡힌다.

export type ScheduleStatus = 'PLANNED' | 'DONE' | 'SKIPPED'
export type RecurrenceFreq = 'DAILY' | 'WEEKLY'
export type DeleteScope = 'FUTURE' | 'ALL'

/** java.time.DayOfWeek 의 이름 */
export type Weekday =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

/** 사용자가 고를 수 있는 분류 목록의 한 항목 (GET /api/categories) */
export interface CategoryResponse {
  id: number
  name: string
  color?: string
  displayOrder: number
  archived: boolean
  createdAt: string
  updatedAt: string
}

/**
 * 일정 조회 응답 한 줄. 1회성 일정도 "회차 하나"로 같은 모양으로 온다.
 *
 * 식별자는 id 하나가 아니라 id + occurrenceDate 다.
 * 반복 일정 하나가 여러 줄로 펼쳐져 오므로 id 는 중복된다.
 *
 * 반복 규칙(recurrence)과 장소(place)는 이 응답에 없다.
 * 규칙은 요청·파싱 타입에만 있고, 장소는 백엔드에 아직 칸이 없다.
 */
export interface ScheduleResponse {
  id: number
  /** "YYYY-MM-DD". 규칙이 만든 원래 날짜라 연기해도 바뀌지 않는다 */
  occurrenceDate: string
  title: string
  /** 마크다운 원문 */
  description?: string
  /** "YYYY-MM-DDTHH:mm:ss". 타임존 표기가 없다 */
  startAt: string
  endAt?: string
  allDay: boolean
  status: ScheduleStatus
  /** 분류 이름을 문자열로 기록한다. 목록에 없는 이름도 들어갈 수 있다 */
  category?: string
  postponeCount: number
  createdAt: string
  updatedAt: string
}

/** 반복 규칙. 이 값이 있으면 반복 일정이 된다 */
export interface RecurrenceRequest {
  freq: RecurrenceFreq
  /** WEEKLY 에서만 쓰인다 */
  byWeekday?: Weekday[]
  /** "YYYY-MM-DD". 없으면 무기한 */
  endsOn?: string
}

export interface ScheduleCreateRequest {
  title: string
  description?: string
  startAt: string
  endAt?: string
  allDay?: boolean
  category?: string
  recurrence?: RecurrenceRequest
}

/** 회차 하나만 고친다. 준 것만 바뀌고, 생략하면 일정의 값으로 되돌아간다 */
export interface OccurrenceUpdateRequest {
  title?: string
  description?: string
  startAt?: string
  endAt?: string
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

export interface UserResponse {
  id: number
  username: string
  displayName?: string
  createdAt: string
}

/** 등록 여부와 마스킹된 꼬리 네 자리만 온다. 키 원문은 서버가 절대 돌려주지 않는다 */
export interface OpenAiKeyStatus {
  registered: boolean
  masked?: string
}

/** 백엔드 GlobalExceptionHandler가 내려주는 에러 형태 */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
