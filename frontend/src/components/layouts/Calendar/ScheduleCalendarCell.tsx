import { occurrenceKey } from "../../../domain/occurrence";
import type { ScheduleResponse } from "../../../types/schedule";

interface Props {
  date: Date;
  /** 이 날짜에 걸린 회차. 부모가 미리 골라 넘긴다 */
  occurrences: ScheduleResponse[];
  /** 이번 달이 아닌 날(앞뒤로 채워진 칸)은 흐리게 표시한다 */
  inCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  onSelect: (date: Date) => void;
}

/** 한 칸에 다 못 넣을 때 "+N" 으로 접는 기준 */
const MAX_VISIBLE = 3;

export default function ScheduleCalendarCell({
  date,
  occurrences,
  inCurrentMonth,
  isToday,
  isSelected,
  onSelect,
}: Props) {
  const visible = occurrences.slice(0, MAX_VISIBLE);
  const hidden = occurrences.length - visible.length;

  return (
    <button
      type="button"
      onClick={() => onSelect(date)}
      aria-label={`${date.getMonth() + 1}월 ${date.getDate()}일, 일정 ${occurrences.length}건`}
      aria-pressed={isSelected}
      className={`flex min-h-24 flex-col gap-1 rounded-md border p-1.5 text-left transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100 ${
        isSelected
          ? "border-brand-500 bg-brand-50"
          : "border-slate-200 bg-white hover:border-brand-200 hover:bg-brand-50/40"
      } ${inCurrentMonth ? "" : "opacity-40"}`}
    >
      <span
        className={`self-start rounded-full px-1.5 text-xs font-semibold ${
          isToday
            ? "bg-brand-500 text-white"
            : inCurrentMonth
              ? "text-slate-600"
              : "text-slate-400"
        }`}
      >
        {date.getDate()}
      </span>

      <ul className="flex list-none flex-col gap-0.5 p-0">
        {visible.map((occurrence) => (
          <li
            key={occurrenceKey(occurrence)}
            // 칸이 좁으므로 한 줄로 자르고, 전체 제목은 title 속성으로 보여준다
            title={
              occurrence.postponeCount > 0
                ? `${occurrence.title} · ${occurrence.postponeCount}번 미룸`
                : occurrence.title
            }
            className={`truncate rounded-sm px-1 text-[11px] leading-4 ${
              occurrence.status === "DONE"
                ? "bg-slate-100 text-slate-400 line-through"
                : "bg-brand-50 text-brand-700"
            }`}
          >
            {occurrence.postponeCount > 0 && (
              <span className="text-amber-600">↻ </span>
            )}
            {occurrence.title}
          </li>
        ))}

        {hidden > 0 && (
          <li className="px-1 text-[11px] leading-4 text-slate-400">
            +{hidden}건
          </li>
        )}
      </ul>
    </button>
  );
}
