import { useCallback, useEffect, useState } from "react";
import {
  createHabitCategory,
  deleteHabitCategory,
  fetchHabitCategories,
  fetchHabits,
  reorderHabitCategories,
} from "../../api/habits";
import OrderButtons from "../layouts/OrderButtons";
import type { Habit, HabitCategory } from "../../types/habit";
import { toLocalDate } from "../../utils/datetime";

const BTN =
  "rounded-md border border-line px-3 py-1.5 text-xs text-ink-soft transition-colors hover:bg-accent-soft";

/**
 * 습관 카테고리를 만들고 지운다.
 * <p>
 * 습관일지가 아니라 설정에 두는 까닭은, 카테고리를 지우면 안의 습관과 지난 기록까지
 * 되돌릴 수 없이 사라지기 때문이다. 매일 보는 화면에 두면 손이 미끄러진다.
 */
export default function HabitCategorySection() {
  const [categories, setCategories] = useState<HabitCategory[]>([]);
  const [habits, setHabits] = useState<Habit[]>([]);
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      /*
       * 습관까지 받는 까닭은 경고에 적을 수를 알아야 해서다.
       * 구간을 오늘 하루로 좁혀 지난 기록까지 끌고 오지 않는다 — 수만 세면 된다
       */
      const today = toLocalDate(new Date());
      const [cats, list] = await Promise.all([
        fetchHabitCategories(),
        fetchHabits(today, today),
      ]);
      setCategories(cats);
      setHabits(list);
      setError(null);
    } catch {
      setError("카테고리를 불러오지 못했습니다");
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const run = async (action: () => Promise<unknown>, fallback: string) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : fallback);
    } finally {
      setBusy(false);
    }
  };

  const add = () => {
    const picked = name.trim();
    if (!picked) return;
    return run(async () => {
      await createHabitCategory({ name: picked });
      setName("");
    }, "카테고리를 만들지 못했습니다");
  };

  const remove = (category: HabitCategory) => {
    /*
     * 그만둔 습관까지 센다. 습관일지에서 내려가 있어도 함께 사라지므로,
     * 보이는 수만 알리면 사람이 생각한 것보다 많이 지워진다
     */
    const count = habits.filter((h) => h.categoryId === category.id).length;

    if (
      !window.confirm(
        count > 0
          ? `"${category.name}" 카테고리를 지우면 안에 든 습관 ${count}개와 그 지난 기록까지 함께 사라집니다. 되돌릴 수 없습니다. 계속할까요?`
          : `"${category.name}" 카테고리를 지울까요?`,
      )
    ) {
      return;
    }
    return run(
      () => deleteHabitCategory(category.id),
      "카테고리를 지우지 못했습니다",
    );
  };

  const categoryIds = categories.map((c) => c.id);

  const move = (ids: number[]) =>
    run(() => reorderHabitCategories(ids), "차례를 바꾸지 못했습니다");

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-line bg-surface p-5 shadow-xs">
      <h3 className="font-semibold text-ink">습관 카테고리</h3>

      <p className="text-xs text-ink-faint">
        습관을 묶는 칸입니다. 지우면 안에 든 습관과 그 지난 기록까지 함께
        사라지고 되돌릴 수 없습니다. 이름은 습관일지에서 제목을 눌러 고칩니다.
      </p>

      {/* 세팅은 걸러 보여 주는 것이 없어 보이는 차례가 곧 무리 전부다 */}
      <ul className="flex list-none flex-col gap-1.5 p-0">
        {categories.map((category) => {
          const count = habits.filter(
            (h) => h.categoryId === category.id,
          ).length;

          return (
            <li
              key={category.id}
              className="flex items-center gap-2 rounded-md border border-line px-3 py-2"
            >
              <span className="min-w-0 flex-1 truncate text-sm text-ink">
                {category.name}
              </span>
              <span className="shrink-0 text-xs text-ink-faint">
                습관 {count}개
              </span>
              {/* 여기 차례가 습관일지의 카테고리 차례가 된다 */}
              <OrderButtons
                ids={categoryIds}
                id={category.id}
                label={category.name}
                busy={busy}
                onMove={(ids) => void move(ids)}
              />
              <button
                type="button"
                onClick={() => void remove(category)}
                // 마지막 하나는 서버가 409 로 막는다. 누르기 전에 알려 둔다
                disabled={busy || categories.length <= 1}
                title={
                  categories.length <= 1
                    ? "마지막 카테고리는 지울 수 없습니다"
                    : "카테고리 삭제"
                }
                className="shrink-0 rounded-md border border-transparent px-2 py-1 text-xs text-danger transition-colors hover:border-danger hover:bg-danger-soft disabled:cursor-not-allowed disabled:text-ink-faint disabled:hover:border-transparent disabled:hover:bg-transparent"
              >
                삭제
              </button>
            </li>
          );
        })}
      </ul>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          void add();
        }}
        className="flex items-center gap-2"
      >
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="예: 살림"
          aria-label="새 카테고리 이름"
          maxLength={50}
          className="min-w-0 flex-1 rounded-md border border-line bg-surface px-2.5 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none"
        />
        <button
          type="submit"
          disabled={busy || !name.trim()}
          className={`shrink-0 ${BTN} disabled:cursor-not-allowed disabled:opacity-50`}
        >
          추가
        </button>
      </form>

      {error && <p className="text-sm text-danger">{error}</p>}
    </section>
  );
}
