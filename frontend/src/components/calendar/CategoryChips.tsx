import { useMemo, useState } from "react";
import { rankCategories, TOP_CATEGORIES } from "../../domain/filter";
import type { CategoryResponse } from "../../types/schedule";

interface Props {
  categories: CategoryResponse[];
  /** 필터 이전의 창 전체 기준 개수 */
  usage: Map<string, number>;
  total: number;
  /** "2026년 8월" */
  monthLabel: string;
  selected: string | null;
  onSelect: (name: string | null) => void;
}

// 두 상태의 클래스를 한곳에 모아둔다
const CHIP_BASE =
  "rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100";
const CHIP_ON = "border-brand-500 bg-brand-500 text-white";
const CHIP_OFF =
  "border-slate-200 bg-white text-slate-500 hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700";

/** 이 달에 많이 쓴 분류를 앞에 놓고, 고른 분류는 항상 보이게 한다 */
export default function CategoryChips({
  categories,
  usage,
  total,
  monthLabel,
  selected,
  onSelect,
}: Props) {
  const [expanded, setExpanded] = useState(false);

  const ranked = useMemo(
    () => rankCategories(usage, categories),
    [usage, categories],
  );

  const top = ranked.slice(0, TOP_CATEGORIES);
  const rest = ranked.slice(TOP_CATEGORIES);

  // 고른 분류가 상위권 밖이면 줄 끝에 붙인다. 활성 필터가 더보기 뒤에 숨으면 안 된다
  const pinned = selected && !top.includes(selected) ? selected : null;

  return (
    <div className="flex flex-col gap-1.5">
      <p className="text-xs font-semibold tracking-wide text-slate-400">
        이 달에 많이 쓴 분류
      </p>

      <div className="flex flex-wrap items-center gap-1.5">
        <button
          type="button"
          onClick={() => onSelect(null)}
          className={`${CHIP_BASE} ${selected === null ? CHIP_ON : CHIP_OFF}`}
        >
          전체 {total}
        </button>

        {top.map((name) => (
          <Chip
            key={name}
            name={name}
            count={usage.get(name) ?? 0}
            selected={selected === name}
            onSelect={onSelect}
          />
        ))}

        {pinned && (
          <Chip
            name={pinned}
            count={usage.get(pinned) ?? 0}
            selected
            onSelect={onSelect}
          />
        )}

        {rest.length > 0 && (
          <button
            type="button"
            onClick={() => setExpanded((v) => !v)}
            aria-expanded={expanded}
            className={`${CHIP_BASE} ${CHIP_OFF}`}
          >
            {expanded ? "접기" : `더보기 (${rest.length})`}
          </button>
        )}
      </div>

      {expanded && (
        <div className="flex flex-col gap-2 rounded-xl border border-slate-200 bg-white p-3">
          <div className="flex flex-wrap items-center gap-1.5">
            {rest.map((name) => (
              <Chip
                key={name}
                name={name}
                count={usage.get(name) ?? 0}
                selected={selected === name}
                onSelect={onSelect}
              />
            ))}
          </div>

          {/* 순위의 근거를 밝힌다. 전체 기간 순위가 아니다 */}
          <p className="text-xs text-slate-400">
            {monthLabel} 화면에 보이는 일정 {total}건 기준입니다.
          </p>
        </div>
      )}
    </div>
  );
}

function Chip({
  name,
  count,
  selected,
  onSelect,
}: {
  name: string;
  count: number;
  selected: boolean;
  onSelect: (name: string | null) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onSelect(selected ? null : name)}
      aria-pressed={selected}
      className={`${CHIP_BASE} ${selected ? CHIP_ON : CHIP_OFF}`}
    >
      {name}{" "}
      <span className={selected ? "text-white/70" : "text-slate-400"}>
        {count}
      </span>
    </button>
  );
}
