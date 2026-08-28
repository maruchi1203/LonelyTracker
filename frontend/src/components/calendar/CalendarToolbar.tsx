import type { CategoryResponse } from "../../types/schedule";
import CategoryChips from "./CategoryChips";

interface Props {
  query: string;
  onQueryChange: (query: string) => void;
  categories: CategoryResponse[];
  usage: Map<string, number>;
  total: number;
  monthLabel: string;
  selectedCategory: string | null;
  onSelectCategory: (name: string | null) => void;
}

/** 검색과 분류 칩. 넓은 필터를 왼쪽에 둔다 */
export default function CalendarToolbar({
  query,
  onQueryChange,
  categories,
  usage,
  total,
  monthLabel,
  selectedCategory,
  onSelectCategory,
}: Props) {
  return (
    <div className="flex flex-wrap items-start gap-4">
      <div className="flex min-w-0 flex-1 basis-56 flex-col gap-1.5">
        <label
          className="text-xs font-semibold tracking-wide text-slate-400"
          htmlFor="schedule-search"
        >
          검색
        </label>
        <input
          id="schedule-search"
          type="search"
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Escape") onQueryChange("");
          }}
          placeholder="제목 · 내용 · 분류"
          className="w-full rounded-full border border-slate-200 bg-white px-3.5 py-1.5 text-sm text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100"
        />
      </div>

      <div className="min-w-0 flex-1 basis-96">
        <CategoryChips
          categories={categories}
          usage={usage}
          total={total}
          monthLabel={monthLabel}
          selected={selectedCategory}
          onSelect={onSelectCategory}
        />
      </div>
    </div>
  );
}
