import { describe, expect, it } from "vitest";
import { describeRecurrence, stepOccurrence } from "./recurrence";
import type { RecurrenceResponse } from "../types/schedule";

const DAILY: RecurrenceResponse = { freq: "DAILY" };
const MWF: RecurrenceResponse = {
  freq: "WEEKLY",
  byWeekday: ["WEDNESDAY", "MONDAY", "FRIDAY"],
};

describe("describeRecurrence", () => {
  it("매일은 그대로 적는다", () => {
    expect(describeRecurrence(DAILY)).toBe("매일");
  });

  it("요일은 준 차례가 아니라 주의 차례로 적는다", () => {
    expect(describeRecurrence(MWF)).toBe("매주 월·수·금");
  });

  it("요일이 비면 매주라고만 적는다", () => {
    expect(describeRecurrence({ freq: "WEEKLY" })).toBe("매주");
  });
});

describe("stepOccurrence", () => {
  it("매일은 하루씩 움직인다", () => {
    expect(stepOccurrence(DAILY, "2026-09-10", 1)).toBe("2026-09-11");
    expect(stepOccurrence(DAILY, "2026-09-10", -1)).toBe("2026-09-09");
  });

  it("달을 넘어도 이어진다", () => {
    expect(stepOccurrence(DAILY, "2026-09-30", 1)).toBe("2026-10-01");
    expect(stepOccurrence(DAILY, "2026-10-01", -1)).toBe("2026-09-30");
  });

  it("규칙에 있는 요일로만 건너뛴다", () => {
    // 2026-09-11 은 금요일. 다음은 월요일이다
    expect(stepOccurrence(MWF, "2026-09-11", 1)).toBe("2026-09-14");
    expect(stepOccurrence(MWF, "2026-09-14", -1)).toBe("2026-09-11");
  });

  it("종료일을 넘어가면 멈춘다", () => {
    const ending: RecurrenceResponse = { freq: "DAILY", endsOn: "2026-09-11" };

    expect(stepOccurrence(ending, "2026-09-10", 1)).toBe("2026-09-11");
    expect(stepOccurrence(ending, "2026-09-11", 1)).toBeNull();
  });

  it("요일이 하나도 없으면 움직이지 않는다", () => {
    expect(stepOccurrence({ freq: "WEEKLY" }, "2026-09-10", 1)).toBeNull();
  });
});
