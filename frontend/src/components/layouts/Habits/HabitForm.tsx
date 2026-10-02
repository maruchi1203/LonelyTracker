import { useState } from "react";
import { HABIT_CATEGORIES } from "../../../domain/habit";
import type {
  Habit,
  HabitCategory,
  HabitCreateRequest,
} from "../../../types/habit";

interface Props {
  category: HabitCategory;
  /** 고치는 중이면 그 습관. 없으면 새로 만드는 것이다 */
  habit?: Habit;
  /**
   * 2분 법칙을 쓰기로 했는지. 끄면 신호 칸(언제·어디서·2분 행동)이 통째로 내려간다.
   * 제목만 남아 가장 단순한 습관 적기가 된다
   */
  twoMinuteRule: boolean;
  onCancel: () => void;
  onSubmit: (body: HabitCreateRequest) => void;
}

const FIELD =
  "w-full rounded-md border border-line bg-surface px-2.5 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none";
const LABEL = "text-xs font-semibold text-ink-soft";

/** 갈래 안에서 바로 적고 고친다. 칸이 적어 모달까지 갈 일이 아니다 */
export default function HabitForm({
  category,
  habit,
  twoMinuteRule,
  onCancel,
  onSubmit,
}: Props) {
  const [title, setTitle] = useState(habit?.title ?? "");
  const [action, setAction] = useState(habit?.twoMinuteAction ?? "");
  const [atTime, setAtTime] = useState(habit?.atTime ?? "");
  const [place, setPlace] = useState(habit?.place ?? "");

  const label = HABIT_CATEGORIES.find((c) => c.value === category)?.label ?? "";

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!title.trim()) return;
        onSubmit({
          title: title.trim(),
          category,
          /*
           * 칸을 내려 둔 채로 저장하면 안 보이는 값이 실려 간다.
           * 다만 고치는 중이라면 이미 적어 둔 값을 지워서는 안 된다 — PUT 이 통째로 덮어쓴다
           */
          twoMinuteAction: twoMinuteRule
            ? action.trim() || undefined
            : habit?.twoMinuteAction,
          atTime: twoMinuteRule ? atTime.trim() || undefined : habit?.atTime,
          place: twoMinuteRule ? place.trim() || undefined : habit?.place,
        });
      }}
      className="flex flex-col gap-2 rounded-md border border-line bg-accent-soft/40 p-3"
    >
      <label className="flex flex-col gap-1">
        <span className={LABEL}>{label} 습관 *</span>
        <input
          autoFocus
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="예: 팔굽혀펴기"
          maxLength={200}
          className={FIELD}
        />
      </label>

      {twoMinuteRule && (
        <>
          <div className="flex flex-wrap gap-2">
            <label className="flex min-w-0 flex-1 basis-32 flex-col gap-1">
              <span className={LABEL}>언제</span>
              <input
                value={atTime}
                onChange={(e) => setAtTime(e.target.value)}
                // 시각으로 묶지 않는 까닭이 여기 보인다. 신호는 시계가 아니라 상황일 때가 많다
                placeholder="예: 퇴근 후"
                maxLength={100}
                className={FIELD}
              />
            </label>

            <label className="flex min-w-0 flex-1 basis-32 flex-col gap-1">
              <span className={LABEL}>어디서</span>
              <input
                value={place}
                onChange={(e) => setPlace(e.target.value)}
                placeholder="예: 거실"
                maxLength={200}
                className={FIELD}
              />
            </label>
          </div>

          <label className="flex flex-col gap-1">
            <span className={LABEL}>2분 행동</span>
            <input
              value={action}
              onChange={(e) => setAction(e.target.value)}
              placeholder="예: 매트 깔기"
              maxLength={200}
              className={FIELD}
            />
          </label>
        </>
      )}

      <div className="flex justify-end gap-1.5">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-md border border-line px-3 py-2 text-sm text-ink-soft hover:bg-surface"
        >
          취소
        </button>
        <button
          type="submit"
          disabled={!title.trim()}
          className="rounded-md bg-accent px-3 py-2 text-sm font-semibold text-canvas transition-colors hover:bg-ink disabled:cursor-not-allowed disabled:opacity-50"
        >
          {habit ? "저장" : "추가"}
        </button>
      </div>
    </form>
  );
}
