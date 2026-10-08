import { useState } from "react";
import type { HabitCategory } from "../../../types/habit";

interface Props {
  category: HabitCategory;
  /** 습관 추가 폼이 열려 있는지 */
  adding: boolean;
  onToggleAdd: () => void;
  onRename: (name: string) => void;
}

const BTN =
  "rounded-md border border-line px-2.5 py-1 text-xs text-ink-soft transition-colors hover:bg-accent-soft";

/**
 * 카테고리 한 칸의 머리.
 * 이름 고치기는 여기서 하고, 만들기와 지우기는 설정에 둔다 — 되돌릴 수 없는 일을
 * 매일 보는 화면에 두면 손이 미끄러진다
 */
export default function CategoryHeader({
  category,
  adding,
  onToggleAdd,
  onRename,
}: Props) {
  /** 고치는 중인 이름. null 이면 고치고 있지 않다 — 빈 글자도 고치는 중일 수 있다 */
  const [draft, setDraft] = useState<string | null>(null);

  if (draft !== null) {
    return (
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const name = draft.trim();

          // 안 바뀐 이름을 보내면 서버가 "이미 있는 카테고리"로 막는다
          if (name && name !== category.name) onRename(name);
          setDraft(null);
        }}
        className="flex items-center gap-1.5"
      >
        <input
          autoFocus
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Escape") setDraft(null);
          }}
          aria-label="카테고리 이름"
          maxLength={50}
          className="min-w-0 flex-1 rounded-md border border-accent bg-surface px-2 py-1 text-sm font-semibold text-ink focus:outline-none"
        />
        <button type="submit" className={BTN}>
          저장
        </button>
        <button type="button" onClick={() => setDraft(null)} className={BTN}>
          취소
        </button>
      </form>
    );
  }

  return (
    <div className="flex items-center justify-between gap-2">
      {/* 이름 자체가 고치기 단추다. 연필을 따로 두면 좁은 머리에 칸이 하나 더 든다 */}
      <button
        type="button"
        onClick={() => setDraft(category.name)}
        title="이름 고치기"
        className="min-w-0 truncate rounded-md px-1 text-left font-semibold text-ink transition-colors hover:bg-accent-soft"
      >
        {category.name}
      </button>

      <button
        type="button"
        onClick={onToggleAdd}
        aria-expanded={adding}
        className={`shrink-0 ${BTN}`}
      >
        + 습관
      </button>
    </div>
  );
}
