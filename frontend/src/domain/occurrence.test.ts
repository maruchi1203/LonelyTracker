import { describe, expect, it } from "vitest";
import { nearestOccurrences } from "./occurrence";
import type { ScheduleResponse } from "../types/schedule";

function occ(
  id: number,
  instanceDate: string | undefined,
  recurring = true,
): ScheduleResponse {
  return {
    id,
    instanceDate,
    title: `#${id}`,
    allDay: false,
    recurring,
    status: "PLANNED",
    createdAt: "2026-09-01T00:00:00",
    updatedAt: "2026-09-01T00:00:00",
  };
}

describe("nearestOccurrences", () => {
  it("오늘 이후의 첫 회차만 남는다", () => {
    const all = [occ(1, "2026-09-08"), occ(1, "2026-09-11"), occ(1, "2026-09-12")];

    expect(nearestOccurrences(all, "2026-09-10")).toEqual([occ(1, "2026-09-11")]);
  });

  it("오늘이 회차면 오늘이 선다", () => {
    const all = [occ(1, "2026-09-10"), occ(1, "2026-09-11")];

    expect(nearestOccurrences(all, "2026-09-10")).toEqual([occ(1, "2026-09-10")]);
  });

  it("전부 지난 달이면 가장 늦은 것이 남는다", () => {
    const all = [occ(1, "2026-08-03"), occ(1, "2026-08-17")];

    expect(nearestOccurrences(all, "2026-09-10")).toEqual([occ(1, "2026-08-17")]);
  });

  it("반복끼리 서로 방해하지 않는다", () => {
    const all = [occ(1, "2026-09-11"), occ(2, "2026-09-12"), occ(1, "2026-09-13")];

    expect(nearestOccurrences(all, "2026-09-10")).toEqual([
      occ(1, "2026-09-11"),
      occ(2, "2026-09-12"),
    ]);
  });

  it("1회성은 전부 그대로 둔다", () => {
    const all = [occ(1, "2026-09-08", false), occ(2, "2026-09-30", false)];

    expect(nearestOccurrences(all, "2026-09-10")).toEqual(all);
  });

  it("날짜 없는 항목도 그대로 둔다", () => {
    const all = [occ(1, undefined, false)];

    expect(nearestOccurrences(all, "2026-09-10")).toEqual(all);
  });
});
