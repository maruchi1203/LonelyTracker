import type { FormVariant } from "./scheduleForm";

/**
 * 받아 둔 초안을 새로고침 뒤에도 지킨다.
 *
 * 문장과 따로 두는 까닭은 값이 다르기 때문이다. 문장은 다시 치면 되지만 초안은
 * 토큰을 써서 받은 것이라, 한 번 잃으면 그 값을 사용자가 다시 낸다.
 *
 * 읽고 쓰는 판단만 여기 둔다. 화면이 들고 있을 모양은 QuickAddContext 가 정한다
 */
export const DRAFT_STATE_KEY = "quickadd-drafts";

/** 지켜 둘 수 있는 상태. 초안이 든 두 가지뿐이다 */
interface DraftsLike {
  mode: "drafts" | "habitDrafts";
  drafts: { saving: boolean }[];
}

/** 초안과 함께 둬야 하는 것. 습관 초안을 일정 탭에 되살리면 넣을 카테고리가 없다 */
export interface Kept<S> {
  variant: FormVariant;
  state: S;
}

/**
 * 지켜 둔 초안을 읽는다. 읽을 것이 없거나 모양이 다르면 없는 것으로 둔다.
 *
 * 저장해 둔 글자를 그대로 믿지 않는 까닭은, 배포로 모양이 바뀌면 옛 글자가 남아
 * 화면이 터지기 때문이다. 초안 하나를 살리려고 앱을 못 쓰게 둘 수는 없다
 */
export function readKept<S extends DraftsLike>(
  store: Pick<Storage, "getItem">,
): Kept<S> | null {
  try {
    const raw = store.getItem(DRAFT_STATE_KEY);
    if (!raw) return null;

    const kept = JSON.parse(raw) as Kept<S>;
    const mode = kept?.state?.mode;
    if (mode !== "drafts" && mode !== "habitDrafts") return null;
    if (!Array.isArray(kept.state.drafts) || kept.state.drafts.length === 0) {
      return null;
    }

    return {
      variant: kept.variant,
      // 저장 중에 창이 닫혔을 수 있다. 그대로 살리면 단추가 영원히 잠긴다
      state: {
        ...kept.state,
        drafts: kept.state.drafts.map((d) => ({ ...d, saving: false })),
      },
    };
  } catch {
    // 저장이 막힌 브라우저이거나 남의 글자가 들었다. 못 지킬 뿐 앱은 돈다
    return null;
  }
}

/**
 * 초안이면 써 두고, 아니면 지운다.
 *
 * beforeunload 를 쓰지 않는 까닭은 sessionStorage 쓰기가 즉시 끝나서다.
 * 바뀔 때마다 써 두면 창이 닫히는 순간에는 이미 저장되어 있다
 */
export function keepDrafts<S extends { mode: string }>(
  store: Pick<Storage, "setItem" | "removeItem">,
  variant: FormVariant,
  state: S,
): void {
  try {
    if (state.mode === "drafts" || state.mode === "habitDrafts") {
      store.setItem(DRAFT_STATE_KEY, JSON.stringify({ variant, state }));
    } else {
      store.removeItem(DRAFT_STATE_KEY);
    }
  } catch {
    // 사생활 보호 창에서는 쓰기가 막힌다
  }
}

/**
 * 지켜 둔 초안을 지금 화면에 되살릴 수 있는지.
 *
 * 습관인지 아닌지만 본다. 일정 초안은 리스트와 달력이 같은 폼을 쓰지만,
 * 습관 초안은 카테고리를 고르는 칸이 있어 습관일지 밖에서는 채울 수가 없다
 */
export function fitsHere(kept: FormVariant, here: FormVariant): boolean {
  return (kept === "habit") === (here === "habit");
}
