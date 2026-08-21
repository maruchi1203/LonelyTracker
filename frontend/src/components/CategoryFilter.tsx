import type { Category } from "../types/schedule";

interface Props {
  categories: Category[];
  selected: string | null;
  onSelect: (name: string | null) => void;
}

// 선택 여부에 따라 카테고리 칩의 모양이 바뀐다
// 두 상태의 클래스를 한곳에 모아둔다
const CHIP_BASE =
  "rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100";
const CHIP_ON = "border-brand-500 bg-brand-500 text-white";
const CHIP_OFF =
  "border-slate-200 bg-white text-slate-500 hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700";

export default function CategoryFilter({
  categories,
  selected,
  onSelect,
}: Props) {
  if (categories.length === 0) return null;

  return (
    <nav className="mb-6 flex flex-wrap gap-1.5" aria-label="카테고리 필터">
      <button
        type="button"
        className={`${CHIP_BASE} ${selected === null ? CHIP_ON : CHIP_OFF}`}
        onClick={() => onSelect(null)}
      >
        전체
      </button>

      {categories
        .filter((c) => !c.archived)
        .map((category) => (
          <button
            key={category.id}
            type="button"
            className={`${CHIP_BASE} ${
              selected === category.name ? CHIP_ON : CHIP_OFF
            }`}
            // 사용자가 지정한 색은 값이 정해져 있지 않아 유틸리티로 만들 수 없다
            style={{ borderColor: category.color ?? undefined }}
            onClick={() => onSelect(category.name)}
          >
            {category.name}
          </button>
        ))}
    </nav>
  );
}
