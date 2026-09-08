// 백엔드 DTO와 짝을 맞춘 타입. 이름도 백엔드를 따른다.
// 필드명을 틀리면 컴파일 단계에서 잡힌다.

export type ScheduleStatus = "PLANNED" | "DONE" | "SKIPPED";
export type RecurrenceFreq = "DAILY" | "WEEKLY";
export type DeleteScope = "FUTURE" | "ALL";

/** java.time.DayOfWeek에 대응되는 요일 이름 */
export type Weekday =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

/**
 * 일정 조회 (회차 하나)
 *
 * 식별자는 id 하나가 아니라 id + instanceDate다 (반복 일정은 id가 같아 회차를 구별할 정보가 instanceDate뿐)
 *
 * 반복 규칙(recurrence)은 이 응답에 없다. 요청·파싱 타입에만 있다.
 */
export interface ScheduleResponse {
  id: number;
  /**
   * "YYYY-MM-DD". 규칙이 만든 원래 날짜라 옮겨도 바뀌지 않는다
   * 없으면 날짜를 안 정한 항목이라 회차가 없다
   */
  instanceDate?: string;
  title: string;
  /** 마크다운 원문 */
  description?: string;
  /** "YYYY-MM-DDTHH:mm:ss". 타임존 표기가 없다. 없으면 리스트에만 있는 항목이다 */
  startAt?: string;
  endAt?: string;
  allDay: boolean;
  /** 습관(반복)의 회차인지. 완료를 어느 경로로 보낼지가 여기서 갈린다 */
  recurring: boolean;
  status: ScheduleStatus;
  /** 태그도 장소처럼 일정 단위 값이라 어느 회차를 봐도 같다 */
  tags?: string[];
  /** 일정 단위 값이라 어느 회차를 봐도 같다 */
  place?: string;
  /** 시작에 필요한 2분 이내의 미니 행동. 이것도 일정 단위 값이다 */
  twoMinuteAction?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 리스트 탭의 한 줄. 회차가 아니라 일정 자체다.
 * 습관은 이 목록에 오지 않아 회차도 상태도 없다.
 */
export interface ScheduleListItem {
  id: number;
  /** 상위 일정. 없으면 최상위다 */
  parentId?: number;
  /** 형제 사이의 순서. 아직 정한 적이 없으면 0이다 */
  displayOrder: number;
  title: string;
  description?: string;
  /** "YYYY-MM-DD". 언제까지 해내야 하나 */
  dueOn?: string;
  /** "YYYY-MM-DDTHH:mm:ss". 없으면 아직 언제 할지 안 정한 항목이다 */
  startAt?: string;
  /** 값이 있으면 완료다 */
  completedAt?: string;
  tags?: string[];
  place?: string;
  twoMinuteAction?: string;
  createdAt: string;
  updatedAt: string;
}

/** 반복 규칙. 이 값이 있으면 반복 일정이 된다 */
export interface RecurrenceRequest {
  freq: RecurrenceFreq;
  /** WEEKLY 에서만 쓰인다 */
  byWeekday?: Weekday[];
  /** "YYYY-MM-DD". 없으면 무기한 */
  endsOn?: string;
}

export interface ScheduleCreateRequest {
  title: string;
  description?: string;
  startAt?: string;
  endAt?: string;
  allDay?: boolean;
  tags?: string[];
  place?: string;
  twoMinuteAction?: string;
  /** 상위 일정. 3단을 넘기면 서버가 들어갈 수 있는 자리로 눌러 앉힌다 */
  parentId?: number;
  /** "YYYY-MM-DD" */
  dueOn?: string;
  recurrence?: RecurrenceRequest;
}

/** 회차 하나만 고친다. 준 것만 바뀌고, 생략하면 일정의 값으로 되돌아간다 */
export interface InstanceUpdateRequest {
  title?: string;
  description?: string;
  startAt?: string;
  endAt?: string;
}

/** 목록 조회 조건. 준 것만 AND로 걸린다. */
export interface ScheduleQuery {
  from?: string;
  to?: string;
  status?: ScheduleStatus;
  /** 이 태그가 붙은 일정만 */
  tag?: string;
}

export interface UserResponse {
  id: number;
  username: string;
  displayName?: string;
  createdAt: string;
}

/** 등록 여부와 마스킹된 꼬리 네 자리만 온다. 키 원문은 서버가 절대 돌려주지 않는다 */
export interface OpenAiKeyStatus {
  registered: boolean;
  masked?: string;
}

/** 사용자 설정 (GET/PUT /api/users/me/settings) */
export interface UserSettings {
  /** 2분 행동 칸을 폼에 띄울지 */
  twoMinuteRule: boolean;
}

/** 백엔드 GlobalExceptionHandler가 내려주는 에러 형태 */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
