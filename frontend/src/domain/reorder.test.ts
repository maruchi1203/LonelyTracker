import { describe, expect, it } from "vitest";
import { applyVisible, canMove, moveBy } from "./reorder";

/**
 * 서버는 무리의 전부를 받고 하나라도 어긋나면 거절한다. 그래서 여기서 틀리면
 * 사용자가 보는 것은 "그 무리의 전부를 한 번에 보내 주세요" 뿐이다.
 */
describe("moveBy", () => {
  it("앞으로 한 칸 옮긴다", () => {
    expect(moveBy([12, 7, 30], 7, -1)).toEqual([7, 12, 30]);
  });

  it("뒤로 한 칸 옮긴다", () => {
    expect(moveBy([12, 7, 30], 7, 1)).toEqual([12, 30, 7]);
  });

  it("맨 앞을 더 올리라고 하면 그대로 둔다", () => {
    expect(moveBy([12, 7, 30], 12, -1)).toEqual([12, 7, 30]);
  });

  it("맨 뒤를 더 내리라고 하면 그대로 둔다", () => {
    expect(moveBy([12, 7, 30], 30, 1)).toEqual([12, 7, 30]);
  });

  /*
   * 다른 탭이 지운 뒤에 단추를 누르면 이리 온다.
   * 자리로 계산하면 -1 이 맨 뒤를 가리켜 멀쩡한 id 가 빠진 목록이 서버로 간다
   */
  it("없는 id 는 어느 방향이든 그대로 둔다", () => {
    expect(moveBy([12, 7, 30], 99, 1)).toEqual([12, 7, 30]);
    expect(moveBy([12, 7, 30], 99, -1)).toEqual([12, 7, 30]);
  });

  it("하나뿐인 무리는 움직일 데가 없다", () => {
    expect(moveBy([5], 5, -1)).toEqual([5]);
    expect(moveBy([5], 5, 1)).toEqual([5]);
  });

  it("둘이면 맞바꾼다", () => {
    expect(moveBy([5, 9], 9, -1)).toEqual([9, 5]);
  });

  // 받은 목록을 화면이 그대로 그리고 있다. 고치면 다시 그리기 전에 어긋난다
  it("원본을 고치지 않는다", () => {
    const ids = [12, 7, 30];
    moveBy(ids, 7, 1);
    expect(ids).toEqual([12, 7, 30]);
  });
});

describe("applyVisible", () => {
  // 7 은 숨은 것(그만둔 습관)이다. 보이는 12·30 만 차례를 바꾼다
  it("숨은 것은 제 자리에 둔다", () => {
    expect(applyVisible([12, 7, 30], [30, 12])).toEqual([30, 7, 12]);
  });

  it("숨은 것이 없으면 받은 차례 그대로다", () => {
    expect(applyVisible([12, 7, 30], [30, 12, 7])).toEqual([30, 12, 7]);
  });

  it("보이는 것이 하나면 아무것도 바뀌지 않는다", () => {
    expect(applyVisible([12, 7, 30], [7])).toEqual([12, 7, 30]);
  });

  /*
   * 화면이 숨긴 것을 사이에 두고 옮긴 경우.
   * 서버로 가는 목록은 길이와 구성원이 그대로여야 거절당하지 않는다
   */
  it("무리의 구성원을 늘리거나 줄이지 않는다", () => {
    const all = [1, 2, 3, 4, 5];
    const moved = applyVisible(all, moveBy([1, 3, 5], 5, -1));
    expect(moved).toEqual([1, 2, 5, 4, 3]);
    expect([...moved].sort()).toEqual([...all].sort());
  });
});

describe("canMove", () => {
  it("끝에서는 그 방향으로만 막는다", () => {
    expect(canMove([12, 7, 30], 12, -1)).toBe(false);
    expect(canMove([12, 7, 30], 12, 1)).toBe(true);
    expect(canMove([12, 7, 30], 30, 1)).toBe(false);
    expect(canMove([12, 7, 30], 30, -1)).toBe(true);
  });

  it("없는 id 는 움직일 수 없다", () => {
    expect(canMove([12, 7, 30], 99, 1)).toBe(false);
  });

  // 단추를 잠그는 판단과 실제로 움직이는 판단이 갈리면 눌러도 아무 일이 없다
  it("막은 자리에서는 moveBy 도 차례를 바꾸지 않는다", () => {
    const ids = [12, 7, 30];

    for (const id of ids) {
      for (const step of [-1, 1] as const) {
        if (!canMove(ids, id, step)) {
          expect(moveBy(ids, id, step)).toEqual(ids);
        }
      }
    }
  });
});
