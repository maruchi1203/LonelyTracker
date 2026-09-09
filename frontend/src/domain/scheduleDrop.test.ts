import { describe, expect, it } from "vitest";
import { dropIntentAt, planDrop } from "./scheduleDrop";
import type { ScheduleListItem } from "../types/schedule";

/** 필요한 칸만 채운 리스트 항목 */
function item(
  id: number,
  extra: Partial<ScheduleListItem> = {},
): ScheduleListItem {
  return {
    id,
    displayOrder: 0,
    title: `#${id}`,
    recurring: false,
    createdAt: "2026-09-09T00:00:00",
    updatedAt: "2026-09-09T00:00:00",
    ...extra,
  };
}

describe("dropIntentAt", () => {
  it("위아래 끝은 형제로, 가운데는 자식으로 본다", () => {
    expect(dropIntentAt(0.1)).toBe("before");
    expect(dropIntentAt(0.5)).toBe("inside");
    expect(dropIntentAt(0.9)).toBe("after");
  });

  it("가운데 띠가 행의 절반이다", () => {
    expect(dropIntentAt(0.24)).toBe("before");
    expect(dropIntentAt(0.26)).toBe("inside");
    expect(dropIntentAt(0.74)).toBe("inside");
    expect(dropIntentAt(0.76)).toBe("after");
  });
});

describe("planDrop", () => {
  it("같은 무리 안에서 앞자리에 끼운다", () => {
    const items = [item(1), item(2), item(3)];

    expect(planDrop(items, 3, 1, "before")).toEqual({
      parentId: null,
      ids: [3, 1, 2],
    });
  });

  it("뒷자리에 끼운다", () => {
    const items = [item(1), item(2), item(3)];

    expect(planDrop(items, 1, 2, "after")).toEqual({
      parentId: null,
      ids: [2, 1, 3],
    });
  });

  it("가운데에 놓으면 그 행의 자식이 된다", () => {
    const items = [item(1), item(2)];

    // 무리가 바뀌므로 대상의 자식 무리를 통째로 보낸다
    expect(planDrop(items, 2, 1, "inside")).toEqual({
      parentId: 1,
      ids: [2],
    });
  });

  it("자식 무리로 데려오면 원래 있던 자식까지 함께 싣는다", () => {
    const items = [item(1), item(2, { parentId: 1 }), item(3)];

    expect(planDrop(items, 3, 1, "inside")).toEqual({
      parentId: 1,
      ids: [2, 3],
    });
  });

  it("다른 무리의 형제로 끼울 때도 그 무리 전부를 싣는다", () => {
    const items = [item(1), item(2, { parentId: 1 }), item(3)];

    expect(planDrop(items, 3, 2, "before")).toEqual({
      parentId: 1,
      ids: [3, 2],
    });
  });

  it("자기 자손 밑으로는 들어가지 않는다", () => {
    const items = [item(1), item(2, { parentId: 1 })];

    // 가지가 통째로 트리에서 떨어져 나간다
    expect(planDrop(items, 1, 2, "inside")).toBeNull();
  });

  it("자기 자신 위에 놓으면 아무 일도 없다", () => {
    expect(planDrop([item(1)], 1, 1, "inside")).toBeNull();
  });

  it("3단 밑으로는 넣지 않는다", () => {
    const items = [
      item(1),
      item(2, { parentId: 1 }),
      item(3, { parentId: 2 }),
      item(4),
    ];

    expect(planDrop(items, 4, 3, "inside")).toBeNull();

    // 3단의 형제 자리는 아직 남아 있다
    expect(planDrop(items, 4, 3, "before")).toEqual({
      parentId: 2,
      ids: [4, 3],
    });
  });

  it("습관은 계층에 끼지 않는다", () => {
    const items = [item(1), item(2, { recurring: true })];

    expect(planDrop(items, 2, 1, "inside")).toBeNull();
    expect(planDrop(items, 1, 2, "inside")).toBeNull();
  });

  it("습관도 최상위 무리에서는 자리를 옮긴다", () => {
    const items = [item(1), item(2, { recurring: true })];

    expect(planDrop(items, 2, 1, "before")).toEqual({
      parentId: null,
      ids: [2, 1],
    });
  });
});
