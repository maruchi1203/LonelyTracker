import type { CategoryResponse } from "../../types/schedule";

interface Props {
  categories: CategoryResponse[];
  selected: string | null;
  onSelect: (name: string | null) => void;
}

// 두 상태의 클래스를 한곳에 모아둔다
const CHIP_BASE =
  "rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100";
const CHIP_ON = "border-brand-500 bg-brand-500 text-white";
const CHIP_OFF =
  "border-slate-200 bg-white text-slate-500 hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700";

/** 분류로 일정을 좁힌다 */
export default function CategoryChips({
  categories,
  selected,
  onSelect,
}: Props) {
  const visible = categories.filter((c) => !c.archived);

  return (
    <div className="flex flex-wrap items-center gap-1.5">
      <button
        type="button"
        onClick={() => onSelect(null)}
        className={`${CHIP_BASE} ${selected === null ? CHIP_ON : CHIP_OFF}`}
      >
        전체
      </button>

      {visible.map((category) => (
        <button
          key={category.id}
          type="button"
          onClick={() => onSelect(category.name)}
          className={`${CHIP_BASE} ${
            selected === category.name ? CHIP_ON : CHIP_OFF
          }`}
        >
          {category.name}
        </button>
      ))}
    </div>
  );
}
