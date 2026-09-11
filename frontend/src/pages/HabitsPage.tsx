import { useCallback, useEffect, useMemo, useState } from "react";
import {
  archiveHabit,
  createHabit,
  deleteHabit,
  fetchHabits,
  logHabit,
} from "../api/habits";
import {
  groupByCategory,
  HABIT_CATEGORIES,
  recentDays,
  streakOf,
} from "../domain/habit";
import type { Habit, HabitCategory } from "../types/habit";
import { toLocalDate } from "../utils/datetime";

/** 한 줄에 보여줄 날 수. 좁은 화면에서도 한 줄에 들어간다 */
const DAYS = 7;

const CELL = "size-7 rounded-md border text-xs transition-colors";
const CELL_DONE = "border-brand-500 bg-brand-500 text-white";
const CELL_TODO = "border-slate-200 text-slate-300 hover:bg-brand-50";

export default function HabitsPage() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [adding, setAdding] = useState<HabitCategory | null>(null);
  const [showArchived, setShowArchived] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const today = toLocalDate(new Date());
  const days = useMemo(() => recentDays(today, DAYS), [today]);

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      setHabits(await fetchHabits(recentDays(toLocalDate(new Date()), DAYS)[0]));
      setError(null);
    } catch (e) {
      fail(e, "습관을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const groups = useMemo(
    () => groupByCategory(habits.filter((h) => showArchived || !h.archived)),
    [habits, showArchived],
  );

  // 여섯 갈래를 고루 채우기를 권한다. 비어 있는 갈래를 세어 알린다
  const emptyCount = groupByCategory(habits.filter((h) => !h.archived)).filter(
    (g) => g.habits.length === 0,
  ).length;

  const handleToggle = async (habit: Habit, onDate: string) => {
    setError(null);
    try {
      await logHabit(habit.id, onDate, !habit.doneDates.includes(onDate));
      await reload();
    } catch (e) {
      fail(e, "기록을 바꾸지 못했습니다");
    }
  };

  const handleAdd = async (body: {
    title: string;
    category: HabitCategory;
    twoMinuteAction?: string;
  }) => {
    setError(null);
    try {
      await createHabit(body);
      setAdding(null);
      await reload();
    } catch (e) {
      fail(e, "습관을 만들지 못했습니다");
    }
  };

  const handleArchive = async (habit: Habit) => {
    setError(null);
    try {
      await archiveHabit(habit.id, !habit.archived);
      await reload();
    } catch (e) {
      fail(e, "상태를 바꾸지 못했습니다");
    }
  };

  const handleDelete = async (habit: Habit) => {
    if (
      !window.confirm(
        `"${habit.title}" 을(를) 지울까요? 지난 기록도 함께 사라집니다.`,
      )
    ) {
      return;
    }

    setError(null);
    try {
      await deleteHabit(habit.id);
      await reload();
    } catch (e) {
      fail(e, "지우지 못했습니다");
    }
  };

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-slate-800">습관일지</h2>

        <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-600 select-none">
          <input
            type="checkbox"
            checked={showArchived}
            onChange={(e) => setShowArchived(e.target.checked)}
            className="size-4 cursor-pointer accent-brand-500"
          />
          그만둔 것도 보기
        </label>
      </div>

      <p className="text-xs text-slate-400">
        2분 안에 시작할 수 있는 행동을 적어 두면 실행될 확률이 높아집니다.
        {emptyCount > 0 &&
          ` 아직 비어 있는 갈래가 ${emptyCount}개 있습니다 — 갈래마다 하나씩 두는 것을 권합니다.`}
      </p>

      {error && (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </p>
      )}

      {loading ? (
        <p className="px-5 py-8 text-center text-sm text-slate-400">
          불러오는 중입니다…
        </p>
      ) : (
        groups.map((group) => (
          <section
            key={group.category}
            className="flex flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-4 shadow-xs"
          >
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-semibold text-slate-700">{group.label}</h3>

              <button
                type="button"
                onClick={() =>
                  setAdding(adding === group.category ? null : group.category)
                }
                aria-expanded={adding === group.category}
                className="rounded-md border border-slate-200 px-2.5 py-1 text-xs text-slate-600 transition-colors hover:bg-brand-50"
              >
                + 습관
              </button>
            </div>

            {group.habits.length === 0 && adding !== group.category && (
              <p className="rounded-md border border-dashed border-slate-200 px-3 py-3 text-center text-xs text-slate-400">
                아직 없습니다. 2분이면 되는 것부터 하나 두어 보세요.
              </p>
            )}

            {adding === group.category && (
              <AddHabitForm
                category={group.category}
                onCancel={() => setAdding(null)}
                onSubmit={handleAdd}
              />
            )}

            {group.habits.length > 0 && (
              <ul className="flex list-none flex-col gap-1 p-0">
                {group.habits.map((habit) => (
                  <HabitRow
                    key={habit.id}
                    habit={habit}
                    days={days}
                    today={today}
                    onToggle={(onDate) => void handleToggle(habit, onDate)}
                    onArchive={() => void handleArchive(habit)}
                    onDelete={() => void handleDelete(habit)}
                  />
                ))}
              </ul>
            )}
          </section>
        ))
      )}
    </div>
  );
}

interface RowProps {
  habit: Habit;
  days: string[];
  today: string;
  onToggle: (onDate: string) => void;
  onArchive: () => void;
  onDelete: () => void;
}

function HabitRow({
  habit,
  days,
  today,
  onToggle,
  onArchive,
  onDelete,
}: RowProps) {
  const streak = streakOf(habit, today);

  return (
    <li
      className={`flex flex-wrap items-center gap-3 rounded-md px-1 py-2 ${
        habit.archived ? "opacity-50" : ""
      }`}
    >
      <div className="flex min-w-0 flex-1 basis-48 flex-col">
        <span className="truncate text-sm text-slate-800">{habit.title}</span>
        {habit.twoMinuteAction && (
          <span className="truncate text-xs text-slate-400">
            2분: {habit.twoMinuteAction}
          </span>
        )}
      </div>

      {streak > 0 && (
        <span
          title="오늘까지 이어 온 날"
          className="shrink-0 rounded-full bg-brand-50 px-2 py-0.5 text-xs text-brand-700"
        >
          {streak}일째
        </span>
      )}

      <div className="flex shrink-0 gap-1">
        {days.map((day) => {
          const done = habit.doneDates.includes(day);
          return (
            <button
              key={day}
              type="button"
              onClick={() => onToggle(day)}
              aria-pressed={done}
              aria-label={`${habit.title} ${day}`}
              title={day}
              className={`${CELL} ${done ? CELL_DONE : CELL_TODO} ${
                day === today ? "ring-2 ring-brand-200" : ""
              }`}
            >
              {Number(day.slice(8))}
            </button>
          );
        })}
      </div>

      <div className="flex shrink-0 gap-1">
        <button
          type="button"
          onClick={onArchive}
          className="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-500 transition-colors hover:bg-slate-50"
        >
          {habit.archived ? "다시" : "그만"}
        </button>
        <button
          type="button"
          onClick={onDelete}
          className="rounded-md border border-transparent px-2 py-1 text-xs text-red-500 transition-colors hover:border-red-200 hover:bg-red-50"
        >
          삭제
        </button>
      </div>
    </li>
  );
}

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
function AddHabitForm({ category, onCancel, onSubmit }: AddProps) {
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
      className="flex flex-wrap items-end gap-2 rounded-md border border-brand-200 bg-brand-50/40 p-3"
    >
      <label className="flex min-w-0 flex-1 basis-48 flex-col gap-1">
        <span className="text-xs font-semibold text-slate-500">
          {label} 습관 *
        </span>
        <input
          autoFocus
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="예: 팔굽혀펴기"
          maxLength={200}
          className="w-full rounded-md border border-slate-200 bg-white px-2.5 py-2 text-sm text-slate-800 focus:border-brand-500 focus:outline-none"
        />
      </label>

      <label className="flex min-w-0 flex-1 basis-48 flex-col gap-1">
        <span className="text-xs font-semibold text-slate-500">2분 행동</span>
        <input
          value={action}
          onChange={(e) => setAction(e.target.value)}
          placeholder="예: 매트 깔기"
          maxLength={200}
          className="w-full rounded-md border border-slate-200 bg-white px-2.5 py-2 text-sm text-slate-800 focus:border-brand-500 focus:outline-none"
        />
      </label>

      <div className="flex gap-1.5">
        <button
          type="submit"
          disabled={!title.trim()}
          className="rounded-md bg-brand-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          추가
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-600 hover:bg-white"
        >
          취소
        </button>
      </div>
    </form>
  );
}
