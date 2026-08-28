import { useCallback, useEffect, useState } from "react";
import {
  createCategory,
  deleteCategory,
  fetchCategories,
  renameCategory,
} from "../../api/categories";
import type { CategoryResponse } from "../../types/schedule";

const INPUT =
  "rounded-md border border-slate-200 bg-white px-2.5 py-2 text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100";

/** 분류 목록은 후보일 뿐이라, 지워도 그 분류를 쓰던 일정은 남는다 */
export default function CategorySection() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setCategories(await fetchCategories());
    } catch (e) {
      setError(e instanceof Error ? e.message : "분류를 불러오지 못했습니다");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const run = async (action: () => Promise<unknown>, fallback: string) => {
    setError(null);
    try {
      await action();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : fallback);
    }
  };

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
      <h3 className="font-semibold text-slate-800">분류</h3>
      <p className="text-sm text-slate-500">
        일정을 만들 때 고를 수 있는 후보입니다. 지워도 그 분류를 쓰던 일정은
        그대로 남습니다.
      </p>

      <ul className="flex list-none flex-col gap-1.5 p-0">
        {categories.map((category) => (
          <li
            key={category.id}
            className="flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2"
          >
            <span
              className="size-2 shrink-0 rounded-full"
              style={{
                backgroundColor: category.color ?? "var(--color-brand-300)",
              }}
            />
            <span className="min-w-0 flex-1 truncate text-sm text-slate-700">
              {category.name}
            </span>

            <button
              type="button"
              className="rounded-md px-2 py-1 text-xs text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              onClick={() => {
                const next = window.prompt("새 이름", category.name);
                if (next && next !== category.name) {
                  void run(
                    () => renameCategory(category.id, next),
                    "이름을 바꾸지 못했습니다",
                  );
                }
              }}
            >
              이름 변경
            </button>

            <button
              type="button"
              className="rounded-md px-2 py-1 text-xs text-slate-400 hover:bg-red-50 hover:text-red-600"
              onClick={() =>
                void run(
                  () => deleteCategory(category.id),
                  "분류를 지우지 못했습니다",
                )
              }
            >
              삭제
            </button>
          </li>
        ))}

        {categories.length === 0 && (
          <li className="text-sm text-slate-400">분류가 없습니다.</li>
        )}
      </ul>

      <form
        className="flex flex-wrap gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          const trimmed = name.trim();
          if (!trimmed) return;
          void run(() => createCategory(trimmed), "분류를 만들지 못했습니다");
          setName("");
        }}
      >
        <input
          className={`${INPUT} min-w-0 flex-1 basis-48`}
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="새 분류 이름"
          maxLength={50}
          aria-label="새 분류 이름"
        />
        <button
          type="submit"
          className="rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:opacity-50"
          disabled={!name.trim()}
        >
          추가
        </button>
      </form>

      {error && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-600">
          {error}
        </p>
      )}
    </section>
  );
}
