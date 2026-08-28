import { useMemo } from "react";
import { groupByStartDate } from "../../../domain/occurrence";
import type { ScheduleResponse } from "../../../types/schedule";
import { toLocalDate } from "../../../utils/datetime";
import { buildMonthDays } from "../../../utils/monthGrid";
import ScheduleCalendarCell from "./ScheduleCalendarCell";

interface Props {
  month: Date;
  onMonthChange: (month: Date) => void;
  selectedDate: Date | null;
  onSelectDate: (date: Date) => void;
  occurrences: ScheduleResponse[];
  loading?: boolean;
}

// 주간, 월간, 연간 (캘린더 형태와 목표를 이 3개로 나눌 예정)
export const CYCLE_UNITS = ["Week", "Month", "Year"] as const;
export type CycleUnit = (typeof CYCLE_UNITS)[number];
const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

// 월간 달력 (주간, 연간 추가 예정)
export default function ScheduleCalendar({
  month,
  onMonthChange,
  selectedDate,
  onSelectDate,
  occurrences,
  loading,
}: Props) {
  const days = useMemo(() => buildMonthDays(month), [month]);
  const byDate = useMemo(() => groupByStartDate(occurrences), [occurrences]);

  const shiftMonth = (delta: number) =>
    onMonthChange(new Date(month.getFullYear(), month.getMonth() + delta, 1));

  const todayKey = toLocalDate(new Date());
  const selectedKey = selectedDate ? toLocalDate(selectedDate) : null;

  return (
    <section className="flex flex-col gap-3">
      <header className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-800">
          {month.getFullYear()}년 {month.getMonth() + 1}월
        </h2>

        <div className="flex items-center gap-1">
          <NavButton label="이전 달" onClick={() => shiftMonth(-1)}>
            ‹
          </NavButton>
          <button
            type="button"
            onClick={() => {
              const now = new Date();
              onMonthChange(new Date(now.getFullYear(), now.getMonth(), 1));
            }}
            className="rounded-md border border-slate-200 px-2.5 py-1 text-xs text-slate-600 transition-colors hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700"
          >
            오늘
          </button>
          <NavButton label="다음 달" onClick={() => shiftMonth(1)}>
            ›
          </NavButton>
        </div>
      </header>

      {/* 로딩 중에도 그리드를 그대로 둔다. 사라지면 이동 화살표가 튄다 */}
      <div
        className={`grid grid-cols-7 gap-1 transition-opacity ${
          loading ? "opacity-50" : ""
        }`}
        aria-busy={loading}
      >
        {WEEKDAYS.map((label, i) => (
          <div
            key={label}
            className={`pb-1 text-center text-xs font-semibold ${
              i === 0
                ? "text-red-400"
                : i === 6
                  ? "text-brand-500"
                  : "text-slate-400"
            }`}
          >
            {label}
          </div>
        ))}

        {days.map((date) => {
          const key = toLocalDate(date);
          return (
            <ScheduleCalendarCell
              key={key}
              date={date}
              occurrences={byDate.get(key) ?? []}
              inCurrentMonth={date.getMonth() === month.getMonth()}
              isToday={key === todayKey}
              isSelected={key === selectedKey}
              onSelect={onSelectDate}
            />
          );
        })}
      </div>
    </section>
  );
}

function NavButton({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className="rounded-md border border-slate-200 px-2.5 py-1 text-slate-600 transition-colors hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700"
    >
      {children}
    </button>
  );
}
