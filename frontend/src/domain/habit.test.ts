import { describe, expect, it } from "vitest";
import { groupByCategory, recentDays, streakOf } from "./habit";
import type { Habit } from "../types/habit";

function habit(overrides: Partial<Habit> = {}): Habit {
  return {
    id: 1,
    title: "명상",
    category: "MIND",
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
  it("여섯 갈래가 늘 자리를 지킨다", () => {
    // 비어 있는 갈래가 보여야 채우기를 권할 수 있다
    const groups = groupByCategory([habit({ category: "BODY" })]);

    expect(groups).toHaveLength(6);
    expect(groups[0].category).toBe("BODY");
    expect(groups[0].habits).toHaveLength(1);
    expect(groups[1].habits).toHaveLength(0);
  });
});
