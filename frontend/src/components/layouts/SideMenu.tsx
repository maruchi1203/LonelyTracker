import type { CategoryResponse } from "../../types/schedule";

interface Props {
  categories: CategoryResponse[];
  selected: string | null;
  onSelect: (name: string | null) => void;
}

const ITEM =
  "flex items-center justify-between gap-2 rounded-md px-2.5 py-1.5 text-sm transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100";
const ITEM_ON = "bg-brand-500 font-medium text-white";
const ITEM_OFF = "text-slate-600 hover:bg-brand-50 hover:text-brand-700";

export default function SideMenu({ categories, selected, onSelect }: Props) {
  const visible = categories.filter((c) => !c.archived);

  return (
    <nav
      aria-label="카테고리"
      className="flex w-52 shrink-0 flex-col gap-1 border-r border-slate-200 pr-4"
    >
      <p className="px-2.5 pb-1 text-xs font-semibold tracking-wide text-slate-400">
        분류
      </p>

      <button
        type="button"
        onClick={() => onSelect(null)}
        className={`${ITEM} ${selected === null ? ITEM_ON : ITEM_OFF}`}
      >
        전체
      </button>

      {visible.map((category) => (
        <button
          key={category.id}
          type="button"
          onClick={() => onSelect(category.name)}
          className={`${ITEM} ${
            selected === category.name ? ITEM_ON : ITEM_OFF
          }`}
        >
          <span className="flex min-w-0 items-center gap-2">
            {/* 사용자가 고른 색은 값이 정해져 있지 않아 유틸리티로 만들 수 없다 */}
            <span
              className="size-2 shrink-0 rounded-full"
              style={{
                backgroundColor: category.color ?? "var(--color-brand-300)",
              }}
            />
            <span className="truncate">{category.name}</span>
          </span>
        </button>
      ))}

      {visible.length === 0 && (
        <p className="px-2.5 text-xs text-slate-400">분류가 없습니다.</p>
      )}
    </nav>
  );
}
