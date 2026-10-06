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
