/**
 * 습관을 묶는 카테고리. 사용자가 만들고 지운다.
 * 이름이 그대로 값이라 코드 칸이 없다 — 습관은 id 로 가리켜 이름을 고쳐도 붙어 있다
 */
export interface HabitCategory {
  id: number;
  name: string;
  /** 화면에 늘어놓는 차례. 목록은 이미 이 순서로 온다 */
  displayOrder: number;
}

/** 카테고리를 만들 때와 이름을 고칠 때가 같은 모양이다 */
export interface HabitCategoryRequest {
  name: string;
}

/** 습관 하나와 그 구간의 기록 */
export interface Habit {
  id: number;
  title: string;
  /** 묶여 있는 카테고리. 이름은 카테고리 목록에서 찾는다 */
  categoryId: number;
  /** 시작에 필요한 2분 이내의 행동. 습관마다 하나다 */
  twoMinuteAction?: string;
  /** 언제 할지. 시각일 수도, "퇴근 후" 같은 상황일 수도 있다 */
  atTime?: string;
  /** 어디서 할지 */
  place?: string;
  displayOrder: number;
  /** 그만둔 습관. 목록에서 내려가되 지난 기록은 남는다 */
  archived: boolean;
  /** "YYYY-MM-DD". 조회 구간 안에서 해낸 날들 */
  doneDates: string[];
  createdAt: string;
  updatedAt: string;
}

export interface HabitCreateRequest {
  title: string;
  categoryId: number;
  twoMinuteAction?: string;
  atTime?: string;
  place?: string;
}

export type HabitUpdateRequest = HabitCreateRequest;

/**
 * 습관 초안이 되묻는 칸. 서버는 ID 만 보내고 문구는 화면이 갖는다.
 *
 * 일정 쪽 ParseQuestion 과 나눠 둔다. 백엔드 enum 은 하나지만 화면마다 오는 것이
 * 정해져 있어, 한 타입에 담으면 쓰이지 않는 짝이 지도마다 생긴다
 */
export type HabitQuestion =
  | "CATEGORY"
  | "CUE_TIME"
  | "PLACE"
  | "TWO_MINUTE"
  | "TOO_VAGUE";

/** 읽어낸 습관 초안 한 장. 저장되지 않았다 */
export interface HabitDraft {
  title: string;
  /** 서버가 이름을 맞춰 준 카테고리. 못 맞췄으면 없고 questions 에 CATEGORY 가 있다 */
  categoryId?: number;
  atTime?: string;
  place?: string;
  twoMinuteAction?: string;
  /** 2분 행동이 문장에 없어 AI 가 지어낸 것인지. 카드가 "제안"이라고 밝힌다 */
  suggestedAction: boolean;
  questions?: HabitQuestion[];
}

/** 부르는 데 성공했으면 200 이다. 읽을 것이 없으면 초안 대신 notice 가 온다 */
export interface HabitParseResponse {
  habits: HabitDraft[];
  notice?: string;
}
