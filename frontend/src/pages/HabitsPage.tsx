import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import {
  archiveHabit,
  createHabit,
  deleteHabit,
  fetchHabitCategories,
  fetchHabits,
  logHabit,
  renameHabitCategory,
  reorderHabits,
  updateHabit,
} from "../api/habits";
import { fetchSettings } from "../api/users";
import { groupByCategory, recentDays } from "../domain/habit";
import { applyVisible } from "../domain/reorder";
import type { Habit, HabitCategory, HabitCreateRequest } from "../types/habit";
import { toLocalDate } from "../utils/datetime";
import { asError } from "../utils/errors";
import { useQuickAddTarget } from "../components/quickadd/QuickAddContext";
import CategoryHeader from "../components/layouts/Habits/CategoryHeader";
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

/** 습관에는 태그가 없다. 자리마다 [] 를 새로 만들지 않도록 한 번만 만든다 */
const NO_TAGS: string[] = [];

export default function HabitsPage() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [categories, setCategories] = useState<HabitCategory[]>([]);
  /** 습관을 적는 중인 카테고리의 id */
  const [adding, setAdding] = useState<number | null>(null);
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

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      // 카테고리를 따로 받는 까닭은, 습관이 하나도 없는 카테고리도 칸을 가져야 해서다
      const [list, cats] = await Promise.all([
        fetchHabits(recentDays(toLocalDate(new Date()), DAYS)[0]),
        fetchHabitCategories(),
      ]);
      setHabits(list);
      setCategories(cats);
      setError(null);
    } catch (thrown) {
      setError(asError(thrown, "습관을 불러오지 못했습니다").message);
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
    () =>
      groupByCategory(
        habits.filter((h) => showArchived || !h.archived),
        categories,
      ),
    [habits, categories, showArchived],
  );

  /** 고치고 나면 늘 다시 받는다. 서버가 눌러 앉힌 값까지 화면에 맞춘다 */
  const run = async <T,>(action: () => Promise<T>, fallback: string) => {
    setError(null);
    try {
      await action();
      await reload();
    } catch (thrown) {
      setError(asError(thrown, fallback).message);
    }
  };

  const handleToggle = (habit: Habit, onDate: string) =>
    run(
      () => logHabit(habit.id, onDate, !habit.doneDates.includes(onDate)),
      "기록을 바꾸지 못했습니다",
    );

  const handleAdd = (body: HabitCreateRequest) =>
    run(async () => {
      await createHabit(body);
      setAdding(null);
    }, "습관을 만들지 못했습니다");

  const handleUpdate = (id: number, body: HabitCreateRequest) =>
    run(async () => {
      await updateHabit(id, body);
      setEditing(null);
    }, "습관을 고치지 못했습니다");

  const handleArchive = (habit: Habit) =>
    run(
      () => archiveHabit(habit.id, !habit.archived),
      "상태를 바꾸지 못했습니다",
    );

  const handleDelete = (habit: Habit) => {
    if (
      !window.confirm(
        `"${habit.title}" 을(를) 지울까요? 지난 기록도 함께 사라집니다.`,
      )
    ) {
      return;
    }
    return run(() => deleteHabit(habit.id), "지우지 못했습니다");
  };

  const handleRenameCategory = (id: number, name: string) =>
    run(() => renameHabitCategory(id, { name }), "이름을 고치지 못했습니다");

  /*
   * 서버는 그 카테고리의 습관 전부를 받는다. 숨긴 것(그만둔 습관)까지 실어야 거절당하지
   * 않고, 숨긴 것은 제 자리에 둬야 다시 켤 때 있던 데가 그대로다
   */
  const handleMoveHabit = (categoryId: number, shownIds: number[]) => {
    const all = habits
      .filter((h) => h.categoryId === categoryId)
      .map((h) => h.id);
    return run(
      () => reorderHabits(categoryId, applyVisible(all, shownIds)),
      "차례를 바꾸지 못했습니다",
    );
  };

  /*
   * 우하단 폼이 이 탭에서는 일정이 아니라 습관을 만들게 한다.
   * 카테고리와 2분 법칙 여부는 이 화면만 알고 있어 걸어 주어야 한다
   */
  useQuickAddTarget(
    {
      defaultDate: null,
      knownTags: NO_TAGS,
      variant: "habit",
      categories,
      twoMinuteRule,
    },
    {
      habit: async (body) => {
        await run(() => createHabit(body), "습관을 만들지 못했습니다");
        // run 이 오류를 배너로 올린다. 폼은 닫히고 목록이 다시 읽힌다
        return true;
      },
    },
  );

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
          "언제·어디서·2분 행동을 적어 두면 실행될 확률이 높아집니다. "}
        {/*
          카테고리를 만들고 지우는 길이 이 화면에 없으므로 어디 있는지는 알려 둬야 한다.
          이름은 칸 머리에서 바로 고친다 — 되돌릴 수 있는 일이라 여기 남겼다
        */}
        카테고리를 추가하거나 삭제하려면{" "}
        <Link to="/settings" className="text-accent hover:underline">
          설정
        </Link>
        으로 가세요. 이름은 카테고리 제목을 눌러 바로 고칩니다.
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
              key={group.category.id}
              className="flex flex-col gap-2 rounded-2xl border border-line bg-surface p-4 shadow-xs"
            >
              <CategoryHeader
                category={group.category}
                adding={adding === group.category.id}
                onToggleAdd={() =>
                  setAdding(
                    adding === group.category.id ? null : group.category.id,
                  )
                }
                onRename={(name) =>
                  void handleRenameCategory(group.category.id, name)
                }
              />

              {group.habits.length === 0 && adding !== group.category.id && (
                <p className="rounded-md border border-dashed border-line px-3 py-3 text-center text-xs text-ink-faint">
                  아직 없습니다. 2분이면 되는 것부터 하나 두어 보세요.
                </p>
              )}

              {adding === group.category.id && (
                <HabitForm
                  category={group.category}
                  twoMinuteRule={twoMinuteRule}
                  onCancel={() => setAdding(null)}
                  onSubmit={(body) => void handleAdd(body)}
                />
              )}

              {group.habits.length > 0 && (
                <ul className="flex list-none flex-col gap-2 p-0">
                  {group.habits.map((habit) =>
                    editing === habit.id ? (
                      // 고치는 동안에는 줄 자리를 폼이 대신한다. 어느 것을 고치는지가 분명하다
                      <li key={habit.id} className="list-none">
                        <HabitForm
                          category={group.category}
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
                        // 보이는 것들끼리만 차례를 바꾼다. 숨긴 것은 handleMoveHabit 이 챈다
                        siblingIds={group.habits.map((h) => h.id)}
                        onMove={(ids) =>
                          void handleMoveHabit(group.category.id, ids)
                        }
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
