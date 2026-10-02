import { useCallback, useEffect, useMemo, useState } from "react";
import {
  archiveHabit,
  createHabit,
  deleteHabit,
  fetchHabits,
  logHabit,
  updateHabit,
} from "../api/habits";
import { fetchSettings } from "../api/users";
import { groupByCategory, recentDays } from "../domain/habit";
import type { Habit, HabitCategory, HabitCreateRequest } from "../types/habit";
import { toLocalDate } from "../utils/datetime";
import HabitRow from "../components/layouts/Habits/HabitRow";
import HabitForm from "../components/layouts/Habits/HabitForm";

/** 한 줄에 보여줄 날 수. 좁은 화면에서도 한 줄에 들어간다 */
const DAYS = 7;

/**
 * 고를 수 있는 열 수. Tailwind 가 쓰는 class 를 미리 적어 둬야 생성된다.
 * 셋으로 나누면 한 칸이 날짜 일곱 개를 못 품어 자리가 좁다
 */
const COLUMNS = [
  { count: 1, style: "grid-cols-1" },
  { count: 2, style: "grid-cols-2" },
];

/** 고른 열 수를 브라우저가 기억한다. 화면 넓이는 사람마다 다르다 */
const COLUMNS_KEY = "habits-columns";

export default function HabitsPage() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [adding, setAdding] = useState<HabitCategory | null>(null);
  /** 고치는 중인 습관의 id. 그 자리에서 폼이 줄을 대신한다 */
  const [editing, setEditing] = useState<number | null>(null);
  const [showArchived, setShowArchived] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /** 설정을 읽기 전에는 2분 법칙을 켠 것으로 본다. 켠 쪽이 기본값이다 */
  const [twoMinuteRule, setTwoMinuteRule] = useState(true);

  const [columns, setColumns] = useState(() => {
    // 쿠키를 막아 둔 브라우저는 읽기에서도 던진다. 쓰기만 감싸 두면 반쪽이다
    try {
      const saved = Number(localStorage.getItem(COLUMNS_KEY));
      return COLUMNS.some((c) => c.count === saved) ? saved : 2;
    } catch {
      return 2;
    }
  });

  const chooseColumns = (count: number) => {
    setColumns(count);
    try {
      localStorage.setItem(COLUMNS_KEY, String(count));
    } catch {
      // 사생활 보호 창에서는 저장이 막힌다. 이번 화면에만 적용되고 끝난다
    }
  };

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

  useEffect(() => {
    // 못 읽어도 화면은 돌아야 한다. 켠 상태로 두면 칸이 더 보일 뿐이다
    fetchSettings()
      .then((s) => setTwoMinuteRule(s.twoMinuteRule))
      .catch(() => undefined);
  }, []);

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

  const handleAdd = async (body: HabitCreateRequest) => {
    setError(null);
    try {
      await createHabit(body);
      setAdding(null);
      await reload();
    } catch (e) {
      fail(e, "습관을 만들지 못했습니다");
    }
  };

  const handleUpdate = async (id: number, body: HabitCreateRequest) => {
    setError(null);
    try {
      await updateHabit(id, body);
      setEditing(null);
      await reload();
    } catch (e) {
      fail(e, "습관을 고치지 못했습니다");
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
        <h2 className="text-lg font-semibold text-ink">습관일지</h2>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1" role="group" aria-label="열 수">
            {COLUMNS.map(({ count }) => (
              <button
                key={count}
                type="button"
                onClick={() => chooseColumns(count)}
                aria-pressed={columns === count}
                title={`${count}열로 보기`}
                className={`size-7 rounded-md border text-xs transition-colors ${
                  columns === count
                    ? "border-accent bg-accent-soft text-accent"
                    : "border-line text-ink-soft hover:bg-surface-soft"
                }`}
              >
                {count}
              </button>
            ))}
          </div>

          <label className="flex cursor-pointer items-center gap-2 text-sm text-ink-soft select-none">
            <input
              type="checkbox"
              checked={showArchived}
              onChange={(e) => setShowArchived(e.target.checked)}
              className="size-4 cursor-pointer accent-accent"
            />
            그만둔 것도 보기
          </label>
        </div>
      </div>

      <p className="text-xs text-ink-faint">
        {twoMinuteRule &&
          "언제·어디서·2분 행동을 적어 두면 실행될 확률이 높아집니다."}
        {emptyCount > 0 &&
          ` 아직 비어 있는 갈래가 ${emptyCount}개 있습니다 — 갈래마다 하나씩 두는 것을 권합니다.`}
      </p>

      {error && (
        <p className="rounded-md border border-danger bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      {loading ? (
        <p className="px-5 py-8 text-center text-sm text-ink-faint">
          불러오는 중입니다…
        </p>
      ) : (
        <div
          className={`grid gap-4 ${
            COLUMNS.find((c) => c.count === columns)?.style ?? "grid-cols-2"
          }`}
        >
          {groups.map((group) => (
          <section
            key={group.category}
            className="flex flex-col gap-2 rounded-2xl border border-line bg-surface p-4 shadow-xs"
          >
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-semibold text-ink">{group.label}</h3>

              <button
                type="button"
                onClick={() =>
                  setAdding(adding === group.category ? null : group.category)
                }
                aria-expanded={adding === group.category}
                className="rounded-md border border-line px-2.5 py-1 text-xs text-ink-soft transition-colors hover:bg-accent-soft"
              >
                + 습관
              </button>
            </div>

            {group.habits.length === 0 && adding !== group.category && (
              <p className="rounded-md border border-dashed border-line px-3 py-3 text-center text-xs text-ink-faint">
                아직 없습니다. 2분이면 되는 것부터 하나 두어 보세요.
              </p>
            )}

            {adding === group.category && (
              <HabitForm
                category={group.category}
                twoMinuteRule={twoMinuteRule}
                onCancel={() => setAdding(null)}
                onSubmit={handleAdd}
              />
            )}

            {group.habits.length > 0 && (
              <ul className="flex list-none flex-col gap-2 p-0">
                {group.habits.map((habit) =>
                  editing === habit.id ? (
                    // 고치는 동안에는 줄 자리를 폼이 대신한다. 어느 것을 고치는지가 분명하다
                    <li key={habit.id} className="list-none">
                      <HabitForm
                        category={habit.category}
                        habit={habit}
                        twoMinuteRule={twoMinuteRule}
                        onCancel={() => setEditing(null)}
                        onSubmit={(body) => void handleUpdate(habit.id, body)}
                      />
                    </li>
                  ) : (
                    <HabitRow
                      key={habit.id}
                      habit={habit}
                      days={days}
                      today={today}
                      twoMinuteRule={twoMinuteRule}
                      onToggle={(onDate) => void handleToggle(habit, onDate)}
                      onEdit={() => setEditing(habit.id)}
                      onArchive={() => void handleArchive(habit)}
                      onDelete={() => void handleDelete(habit)}
                    />
                  ),
                )}
              </ul>
            )}
          </section>
          ))}
        </div>
      )}
    </div>
  );
}
