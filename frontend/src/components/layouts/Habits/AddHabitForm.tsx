import { useState } from "react";
import { HABIT_CATEGORIES } from "../../../domain/habit";
import type { HabitCategory } from "../../../types/habit";

interface AddProps {
  category: HabitCategory;
  onCancel: () => void;
  onSubmit: (body: {
    title: string;
    category: HabitCategory;
    twoMinuteAction?: string;
  }) => void;
}

/** 갈래 안에서 바로 적는다. 칸이 셋뿐이라 모달까지 갈 일이 아니다 */
export default function AddHabitForm({ category, onCancel, onSubmit }: AddProps) {
  const [title, setTitle] = useState("");
  const [action, setAction] = useState("");

  const label = HABIT_CATEGORIES.find((c) => c.value === category)?.label ?? "";

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!title.trim()) return;
        onSubmit({
          title: title.trim(),
          category,
          twoMinuteAction: action.trim() || undefined,
        });
      }}
      className="flex flex-wrap items-end gap-2 rounded-md border border-line bg-accent-soft/40 p-3"
    >
      <label className="flex min-w-0 flex-1 basis-48 flex-col gap-1">
        <span className="text-xs font-semibold text-ink-soft">
          {label} 습관 *
        </span>
        <input
          autoFocus
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="예: 팔굽혀펴기"
          maxLength={200}
          className="w-full rounded-md border border-line bg-surface px-2.5 py-2 text-sm text-ink focus:border-accent focus:outline-none"
        />
      </label>

      <label className="flex min-w-0 flex-1 basis-48 flex-col gap-1">
        <span className="text-xs font-semibold text-ink-soft">2분 행동</span>
        <input
          value={action}
          onChange={(e) => setAction(e.target.value)}
          placeholder="예: 매트 깔기"
          maxLength={200}
          className="w-full rounded-md border border-line bg-surface px-2.5 py-2 text-sm text-ink focus:border-accent focus:outline-none"
        />
      </label>

      <div className="flex gap-1.5">
        <button
          type="submit"
          disabled={!title.trim()}
          className="rounded-md bg-accent px-3 py-2 text-sm font-semibold text-canvas transition-colors hover:bg-ink disabled:cursor-not-allowed disabled:opacity-50"
        >
          추가
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded-md border border-line px-3 py-2 text-sm text-ink-soft hover:bg-surface"
        >
          취소
        </button>
      </div>
    </form>
  );
}
