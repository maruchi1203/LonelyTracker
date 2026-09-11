/** 습관이 기르는 갈래. 여섯 개를 고정으로 둔다 */
export type HabitCategory =
  | "BODY"
  | "MIND"
  | "SIDE_JOB"
  | "ART"
  | "LEARNING"
  | "RELATIONSHIP";

/** 습관 하나와 그 구간의 기록 */
export interface Habit {
  id: number;
  title: string;
  category: HabitCategory;
  /** 시작에 필요한 2분 이내의 행동. 습관마다 하나다 */
  twoMinuteAction?: string;
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
  category: HabitCategory;
  twoMinuteAction?: string;
}

export type HabitUpdateRequest = HabitCreateRequest;
