import { useMemo, useState } from "react";
import { groupByStartDate } from "../../../domain/occurrence";
import type { ScheduleResponse } from "../../../types/schedule";
import { toLocalDate } from "../../../utils/datetime";
import ScheduleCalendarCell from "./ScheduleCalendarCell";

interface Props {
  occurrences: ScheduleResponse[];
  /** 날짜를 선택 시 하단에 해당 날짜의 리스트 생성*/
  onSelectDate?: (date: Date) => void;
}

// 주간, 월간, 연간 (캘린더 형태와 목표를 이 3개로 나눌 예정)
export const CYCLE_UNITS = ["Week", "Month", "Year"] as const;
export type CycleUnit = (typeof CYCLE_UNITS)[number];
const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

/** 한 달 그리드에 놓일 날짜들. 앞뒤 달의 날짜로 첫 주와 마지막 주를 채운다 */
function buildMonthDays(month: Date): Date[] {
  const firstDate = new Date(month.getFullYear(), month.getMonth(), 1);
  const lastDate = new Date(
    month.getFullYear(),
    month.getMonth() + 1,
    0,
  ).getDate();

  const prevMonthDates = firstDate.getDay(); // 1일 앞에 채울 지난달 날짜 수
  const weeks = Math.ceil((prevMonthDates + lastDate) / 7);

  const start = new Date(firstDate);
  start.setDate(firstDate.getDate() - prevMonthDates);

  return Array.from({ length: weeks * 7 }, (_, i) => {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    return d;
  });
}

// 월간 달력 (주간, 연간 추가 예정)
export default function ScheduleCalendar({ occurrences, onSelectDate }: Props) {
  const [month, setMonth] = useState(() => new Date());
  const [selected, setSelected] = useState<Date | null>(null);

  const days = useMemo(() => buildMonthDays(month), [month]);
  const byDate = useMemo(() => groupByStartDate(occurrences), [occurrences]);

  const shiftMonth = (delta: number) =>
    setMonth((m) => new Date(m.getFullYear(), m.getMonth() + delta, 1));

  const handleSelect = (date: Date) => {
    setSelected(date);
    onSelectDate?.(date);
  };

  const todayKey = toLocalDate(new Date());
  const selectedKey = selected ? toLocalDate(selected) : null;

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
            onClick={() => setMonth(new Date())}
            className="rounded-md border border-slate-200 px-2.5 py-1 text-xs text-slate-600 transition-colors hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700"
          >
            오늘
          </button>
          <NavButton label="다음 달" onClick={() => shiftMonth(1)}>
            ›
          </NavButton>
        </div>
      </header>

      <div className="grid grid-cols-7 gap-1">
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
              onSelect={handleSelect}
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
