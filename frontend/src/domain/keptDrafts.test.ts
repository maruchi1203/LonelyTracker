import { beforeEach, describe, expect, it } from "vitest";
import {
  DRAFT_STATE_KEY,
  fitsHere,
  keepDrafts,
  readKept,
} from "./keptDrafts";

/** sessionStorage 를 흉내 낸다. 실제 저장소를 쓰면 테스트끼리 샌다 */
function fakeStore(seed: Record<string, string> = {}) {
  const box = { ...seed };
  return {
    box,
    getItem: (k: string) => box[k] ?? null,
    setItem: (k: string, v: string) => {
      box[k] = v;
    },
    removeItem: (k: string) => {
      delete box[k];
    },
  };
}

interface TestState {
  mode: "drafts" | "habitDrafts";
  drafts: { saving: boolean; title: string }[];
}

const twoDrafts: TestState = {
  mode: "drafts",
  drafts: [
    { saving: false, title: "장보기" },
    { saving: false, title: "청소" },
  ],
};

describe("keepDrafts", () => {
  let store = fakeStore();

  beforeEach(() => {
    store = fakeStore();
  });

  it("초안이면 써 둔다", () => {
    keepDrafts(store, "calendar", twoDrafts);
    expect(JSON.parse(store.box[DRAFT_STATE_KEY])).toEqual({
      variant: "calendar",
      state: twoDrafts,
    });
  });

  // 초안을 다 치웠으면 지켜 둘 것이 없다. 남겨 두면 새로고침에 되살아난다
  it("초안이 아닌 상태는 지운다", () => {
    keepDrafts(store, "calendar", twoDrafts);
    keepDrafts(store, "calendar", { mode: "idle" });
    expect(store.box[DRAFT_STATE_KEY]).toBeUndefined();
  });

  it("오류나 안내도 지켜 두지 않는다", () => {
    keepDrafts(store, "calendar", twoDrafts);
    keepDrafts(store, "calendar", { mode: "error" });
    expect(store.box[DRAFT_STATE_KEY]).toBeUndefined();
  });
});

describe("readKept", () => {
  it("써 둔 것을 그대로 읽는다", () => {
    const store = fakeStore();
    keepDrafts(store, "habit", { ...twoDrafts, mode: "habitDrafts" });

    const kept = readKept<TestState>(store);
    expect(kept?.variant).toBe("habit");
    expect(kept?.state.drafts).toHaveLength(2);
  });

  /*
   * 저장 중에 창이 닫히면 saving 이 true 로 굳는다.
   * 그대로 살리면 단추가 영원히 잠겨 카드를 버릴 수도 없다
   */
  it("저장 중 깃발을 내려 둔다", () => {
    const store = fakeStore({
      [DRAFT_STATE_KEY]: JSON.stringify({
        variant: "calendar",
        state: { mode: "drafts", drafts: [{ saving: true, title: "장보기" }] },
      }),
    });

    expect(readKept<TestState>(store)?.state.drafts[0].saving).toBe(false);
  });

  it("빈 저장소는 null 이다", () => {
    expect(readKept<TestState>(fakeStore())).toBeNull();
  });

  // 배포로 모양이 바뀌면 옛 글자가 남는다. 초안 하나 때문에 앱이 터지면 안 된다
  it("깨진 글자나 모르는 모양은 null 이다", () => {
    expect(
      readKept<TestState>(fakeStore({ [DRAFT_STATE_KEY]: "{{{" })),
    ).toBeNull();

    expect(
      readKept<TestState>(
        fakeStore({ [DRAFT_STATE_KEY]: JSON.stringify({ variant: "calendar" }) }),
      ),
    ).toBeNull();

    expect(
      readKept<TestState>(
        fakeStore({
          [DRAFT_STATE_KEY]: JSON.stringify({
            variant: "calendar",
            state: { mode: "parsing" },
          }),
        }),
      ),
    ).toBeNull();
  });

  it("초안이 하나도 없으면 null 이다", () => {
    const store = fakeStore({
      [DRAFT_STATE_KEY]: JSON.stringify({
        variant: "calendar",
        state: { mode: "drafts", drafts: [] },
      }),
    });

    expect(readKept<TestState>(store)).toBeNull();
  });
});

describe("fitsHere", () => {
  // 습관 초안은 카테고리를 고르는 칸이 있어 습관일지 밖에서는 채울 수 없다
  it("습관 초안은 습관일지에서만 되살린다", () => {
    expect(fitsHere("habit", "habit")).toBe(true);
    expect(fitsHere("habit", "calendar")).toBe(false);
    expect(fitsHere("habit", "list")).toBe(false);
  });

  it("일정 초안은 리스트와 달력을 넘나든다", () => {
    expect(fitsHere("calendar", "list")).toBe(true);
    expect(fitsHere("list", "calendar")).toBe(true);
    expect(fitsHere("calendar", "habit")).toBe(false);
  });
});
