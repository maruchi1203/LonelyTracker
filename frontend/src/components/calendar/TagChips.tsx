import { useMemo, useState } from "react";
import { rankTags, TOP_TAGS } from "../../domain/filter";

interface Props {
  /** 이미 쓴 적 있는 태그. 이번 달에 안 나온 것도 후보로 남긴다 */
  known: string[];
  /** 필터 이전의 창 전체 기준 개수 */
  usage: Map<string, number>;
  total: number;
  /** "2026년 8월" */
  monthLabel: string;
  selected: string | null;
  onSelect: (name: string | null) => void;
}

// 두 상태의 클래스를 한곳에 모아둔다
// shrink-0 이 없으면 한 줄에 몰렸을 때 칩이 찌그러진다
const CHIP_BASE =
  "shrink-0 rounded-full border px-3.5 py-1.5 text-sm font-medium whitespace-nowrap transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100";
const CHIP_ON = "border-brand-500 bg-brand-500 text-white";
const CHIP_OFF =
  "border-slate-200 bg-white text-slate-500 hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700";

/** 이 달에 많이 쓴 태그를 앞에 놓고, 고른 분류는 항상 보이게 한다 */
export default function TagChips({
  known,
  usage,
  total,
  monthLabel,
  selected,
  onSelect,
}: Props) {
  const [expanded, setExpanded] = useState(false);

  const ranked = useMemo(
    () => rankTags(usage, known),
    [usage, known],
  );

  const top = ranked.slice(0, TOP_TAGS);
  const rest = ranked.slice(TOP_TAGS);

  // 고른 태그가 상위권 밖이면 줄 끝에 붙인다. 활성 필터가 더보기 뒤에 숨으면 안 된다
  const pinned = selected && !top.includes(selected) ? selected : null;

  return (
    <div className="flex flex-col gap-1.5">
      <p className="text-xs font-semibold tracking-wide text-slate-400">
        이 달에 많이 쓴 태그
      </p>

      {/* 넘쳐도 줄바꿈하지 않고 가로로 흐른다. py-1 은 포커스 링이 잘리지 않게 둔 자리다 */}
      <div className="flex items-center gap-1.5 overflow-x-auto py-1">
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
