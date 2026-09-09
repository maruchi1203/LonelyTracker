import { describe, expect, it } from "vitest";
import {
  dropIntentAt,
  dropLevelAt,
  planDrop,
  planDropAtEnd,
} from "./scheduleDrop";
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

describe("dropLevelAt", () => {
  it("한 칸마다 한 단씩 깊어진다", () => {
    expect(dropLevelAt(0)).toBe(0);
    expect(dropLevelAt(31)).toBe(0);
    expect(dropLevelAt(32)).toBe(1);
    expect(dropLevelAt(70)).toBe(2);
  });

  it("3단을 넘지 않고 왼쪽으로 벗어나도 최상위다", () => {
    expect(dropLevelAt(500)).toBe(2);
    expect(dropLevelAt(-20)).toBe(0);
  });
});

describe("planDrop", () => {
  it("같은 무리 안에서 앞자리에 끼운다", () => {
    const items = [item(1), item(2), item(3)];

    expect(planDrop(items, 3, 1, "before", 0)).toEqual({
      parentId: null,
      ids: [3, 1, 2],
      level: 0,
    });
  });

  it("뒷자리에 끼운다", () => {
    const items = [item(1), item(2), item(3)];

    expect(planDrop(items, 1, 2, "after", 0)).toEqual({
      parentId: null,
      ids: [2, 1, 3],
      level: 0,
    });
  });

  it("가운데에 놓으면 그 행의 막내 자식이 된다", () => {
    const items = [item(1), item(2, { parentId: 1 }), item(3)];

    expect(planDrop(items, 3, 1, "inside", 0)).toEqual({
      parentId: 1,
      ids: [2, 3],
      level: 1,
    });
  });

  it("오른쪽으로 한 칸 밀면 위 행의 첫 자식이 된다", () => {
    const items = [item(1), item(2, { parentId: 1 }), item(3)];

    // 2 의 앞자리 틈. 그 위 행은 1 이다
    expect(planDrop(items, 3, 2, "before", 1)).toEqual({
      parentId: 1,
      ids: [3, 2],
      level: 1,
    });
  });

  it("왼쪽 끝에 두면 같은 틈이라도 최상위가 된다", () => {
    const items = [item(1), item(2, { parentId: 1 }), item(3)];

    // 1 바로 뒤의 최상위 자리다
    expect(planDrop(items, 3, 2, "before", 0)).toEqual({
      parentId: null,
      ids: [1, 3],
      level: 0,
    });
  });

  it("깊은 행 아래에서 왼쪽으로 빼면 조상의 형제가 된다", () => {
    const items = [
      item(1),
      item(2, { parentId: 1 }),
      item(3, { parentId: 2 }),
      item(4),
    ];

    // 3 의 뒷자리 틈에서 0단을 고르면 1 바로 뒤에 선다
    expect(planDrop(items, 4, 3, "after", 0)).toEqual({
      parentId: null,
      ids: [1, 4],
      level: 0,
    });

    // 같은 틈에서 1단이면 2 바로 뒤다
    expect(planDrop(items, 4, 3, "after", 1)).toEqual({
      parentId: 1,
      ids: [2, 4],
      level: 1,
    });
  });

  it("끌고 있는 가지는 없는 셈 치고 위 행을 센다", () => {
    const items = [item(1), item(2, { parentId: 1 }), item(3)];

    // 2 를 3 의 앞자리로 옮긴다. 위 행은 자기가 아니라 1 이다
    expect(planDrop(items, 2, 3, "before", 0)).toEqual({
      parentId: null,
      ids: [1, 2, 3],
      level: 0,
    });
  });

  it("자기 자손 밑으로는 들어가지 않는다", () => {
    const items = [item(1), item(2, { parentId: 1 })];

    // 가지가 통째로 트리에서 떨어져 나간다
    expect(planDrop(items, 1, 2, "inside", 0)).toBeNull();
  });

  it("자기 자신 위에 놓으면 아무 일도 없다", () => {
    expect(planDrop([item(1)], 1, 1, "inside", 0)).toBeNull();
  });

  it("3단 밑으로는 넣지 않는다", () => {
    const items = [
      item(1),
      item(2, { parentId: 1 }),
      item(3, { parentId: 2 }),
      item(4),
    ];

    expect(planDrop(items, 4, 3, "inside", 0)).toBeNull();

    // 아무리 오른쪽으로 밀어도 3단의 형제까지다
    expect(planDrop(items, 4, 3, "before", 2)).toEqual({
      parentId: 2,
      ids: [4, 3],
      level: 2,
    });
  });

  it("설 수 없는 단까지 밀면 설 수 있는 단을 돌려준다", () => {
    const items = [item(1), item(2)];

    // 위 행이 최상위라 아무리 밀어도 1단까지다
    expect(planDrop(items, 2, 1, "after", 2)).toEqual({
      parentId: 1,
      ids: [2],
      level: 1,
    });
  });

  it("습관 밑으로 밀어도 그 옆자리에 선다", () => {
    const items = [item(1, { recurring: true }), item(2)];

    expect(planDrop(items, 2, 1, "after", 2)).toEqual({
      parentId: null,
      ids: [1, 2],
      level: 0,
    });
  });

  it("습관을 끌면 오른쪽으로 밀어도 최상위에 남는다", () => {
    const items = [item(1), item(2, { recurring: true })];

    expect(planDrop(items, 2, 1, "after", 2)).toEqual({
      parentId: null,
      ids: [1, 2],
      level: 0,
    });
  });

  it("습관은 계층에 끼지 않는다", () => {
    const items = [item(1), item(2, { recurring: true })];

    expect(planDrop(items, 2, 1, "inside", 0)).toBeNull();
    expect(planDrop(items, 1, 2, "inside", 0)).toBeNull();
  });

  it("습관도 최상위 무리에서는 자리를 옮긴다", () => {
    const items = [item(1), item(2, { recurring: true })];

    expect(planDrop(items, 2, 1, "before", 0)).toEqual({
      parentId: null,
      ids: [2, 1],
      level: 0,
    });
  });
});

describe("planDropAtEnd", () => {
  it("깊은 곳에 있던 일정을 최상위 끝으로 뺀다", () => {
    const items = [item(1), item(2, { parentId: 1 }), item(3, { parentId: 2 })];

    expect(planDropAtEnd(items, 3)).toEqual({
      parentId: null,
      ids: [1, 3],
      level: 0,
    });
  });

  it("최상위 안에서도 맨 끝으로 보낸다", () => {
    const items = [item(1), item(2), item(3)];

    expect(planDropAtEnd(items, 1)).toEqual({
      parentId: null,
      ids: [2, 3, 1],
      level: 0,
    });
  });

  it("이미 끝자리면 아무 일도 없다", () => {
    const items = [item(1), item(2)];

    expect(planDropAtEnd(items, 2)).toBeNull();
  });
});
