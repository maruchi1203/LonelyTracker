import { describe, expect, it } from "vitest";
import { groupByCategory, recentDays, streakOf } from "./habit";
import type { Habit, HabitCategory } from "../types/habit";

/** 카테고리 셋. 차례는 서버가 display_order 로 맞춰 준다 */
const CATEGORIES: HabitCategory[] = [
  { id: 1, name: "운동", displayOrder: 0 },
  { id: 2, name: "마음챙김", displayOrder: 1 },
  { id: 3, name: "부업", displayOrder: 2 },
];

function habit(overrides: Partial<Habit> = {}): Habit {
  return {
    id: 1,
    title: "명상",
    categoryId: 2,
    displayOrder: 0,
    archived: false,
    doneDates: [],
    createdAt: "2026-09-01T00:00:00",
    updatedAt: "2026-09-01T00:00:00",
    ...overrides,
  };
}

describe("recentDays", () => {
  it("오늘이 맨 뒤에 온다", () => {
    expect(recentDays("2026-09-11", 3)).toEqual([
      "2026-09-09",
      "2026-09-10",
      "2026-09-11",
    ]);
  });

  it("달을 거슬러도 이어진다", () => {
    expect(recentDays("2026-10-02", 3)).toEqual([
      "2026-09-30",
      "2026-10-01",
      "2026-10-02",
    ]);
  });
});

describe("streakOf", () => {
  it("오늘까지 이어 온 날을 센다", () => {
    const h = habit({ doneDates: ["2026-09-09", "2026-09-10", "2026-09-11"] });

    expect(streakOf(h, "2026-09-11")).toBe(3);
  });

  it("오늘을 아직 안 했어도 어제까지는 살아 있다", () => {
    const h = habit({ doneDates: ["2026-09-09", "2026-09-10"] });

    expect(streakOf(h, "2026-09-11")).toBe(2);
  });

  it("중간이 끊기면 거기서 멈춘다", () => {
    const h = habit({ doneDates: ["2026-09-08", "2026-09-10", "2026-09-11"] });

    expect(streakOf(h, "2026-09-11")).toBe(2);
  });

  it("이틀 전까지만 했으면 끊긴 것이다", () => {
    const h = habit({ doneDates: ["2026-09-08", "2026-09-09"] });

    expect(streakOf(h, "2026-09-11")).toBe(0);
  });

  it("기록이 없으면 0이다", () => {
    expect(streakOf(habit(), "2026-09-11")).toBe(0);
  });
});

describe("groupByCategory", () => {
  it("빈 카테고리도 자리를 지킨다", () => {
    // 빈 카테고리가 보여야 그 자리에 습관을 넣는 단추가 걸린다
    const groups = groupByCategory([habit({ categoryId: 1 })], CATEGORIES);

    expect(groups).toHaveLength(3);
    expect(groups[0].category.name).toBe("운동");
    expect(groups[0].habits).toHaveLength(1);
    expect(groups[1].habits).toHaveLength(0);
  });

  it("차례는 받은 목록을 그대로 따른다", () => {
    const groups = groupByCategory([], [CATEGORIES[2], CATEGORIES[0]]);

    expect(groups.map((g) => g.category.name)).toEqual(["부업", "운동"]);
  });

  it("없는 카테고리를 가리키는 습관은 어디에도 실리지 않는다", () => {
    /*
     * 외래 키가 NOT NULL 이고 카테고리를 지우면 습관도 함께 가므로 실제로는 생기지 않는다.
     * 기타 묶음을 따로 두지 않기로 한 것을 적어 둔다
     */
    const groups = groupByCategory([habit({ categoryId: 999 })], CATEGORIES);

    expect(groups.every((g) => g.habits.length === 0)).toBe(true);
  });
});
