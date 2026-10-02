import { useMemo } from "react";
import { assignLanes } from "../../../domain/calendarLanes";
import { entriesByDate, instanceDateKeys } from "../../../domain/instance";
import type { ScheduleResponse } from "../../../types/schedule";
import { toLocalDate } from "../../../utils/datetime";
import { buildMonthDays } from "../../../utils/monthGrid";
import ScheduleCalendarCell from "./ScheduleCalendarCell";
import WarpBorder from "../WarpBorder";

interface Props {
  month: Date;
  onMonthChange: (month: Date) => void;
  selectedDate: Date | null;
  onSelectDate: (date: Date) => void;
  instances: ScheduleResponse[];
  loading?: boolean;
  /** 펼친 칸에서 일정을 골랐을 때 */
  onPickInstance: (instance: ScheduleResponse) => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
}

// 주간, 월간, 연간 (캘린더 형태와 목표를 이 3개로 나눌 예정)
export const CYCLE_UNITS = ["Week", "Month", "Year"] as const;
export type CycleUnit = (typeof CYCLE_UNITS)[number];
const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];
const DAYS_PER_WEEK = 7;

// 월간 달력 (주간, 연간 추가 예정)
export default function ScheduleCalendar({
  month,
  onMonthChange,
  selectedDate,
  onSelectDate,
  instances,
  loading,
  onPickInstance,
  onToggleStatus,
}: Props) {
  const days = useMemo(() => buildMonthDays(month), [month]);
  const byDate = useMemo(() => assignLanes(days, instances), [days, instances]);

  /*
   * 펼친 칸은 둘로 나뉜다.
   *
   * 여러 날에 걸친 것은 띠로 남아야 일곱 칸을 가로질러 이어진다 — 그 자리는
   * 레인이 정하므로 시각으로 줄 세울 수 없다. 하루 안에 끝나는 것만 시각순 줄이 된다
   */
  const [spanning, sameDay] = useMemo(() => {
    const wide: ScheduleResponse[] = [];
    const short: ScheduleResponse[] = [];
    for (const one of instances) {
      (instanceDateKeys(one).length > 1 ? wide : short).push(one);
    }
    return [wide, short];
  }, [instances]);

  /*
   * 띠는 걸친 일정만 그린다. 기한은 걸치지 않고 하루에만 서므로 줄 쪽으로 넘긴다 —
   * 안 떼면 같은 기한이 띠와 줄 양쪽에 두 번 뜬다
   */
  const bandByDate = useMemo(
    () => assignLanes(days, spanning.map(withoutDue)),
    [days, spanning],
  );
  const perDay = useMemo(
    () => entriesByDate([...sameDay, ...spanning]),
    [sameDay, spanning],
  );

  /**
   * 펼칠 주. 고른 날이 없으면 오늘이 든 주를 편다.
   * 오늘이 이 달 격자에 없으면 첫 주를 편다 — 첫 주에는 늘 1일이 있다
   */
  const openWeek = useMemo(() => {
    const anchor = toLocalDate(selectedDate ?? new Date());
    const at = days.findIndex((d) => toLocalDate(d) === anchor);
    return at === -1 ? 0 : Math.floor(at / DAYS_PER_WEEK);
  }, [days, selectedDate]);

  /**
   * 열 너비. 고른 요일만 넓다.
   *
   * 격자 하나가 요일 머리글부터 마지막 주까지 다 쥐고 있어, 여기 준 너비가
   * 위아래로 그대로 맞는다. 펼친 주에만 따로 주면 그 줄만 어긋나 버린다.
   * 대신 고른 요일은 모든 주에서 넓어진다 — 줄맞춤을 지키려면 치러야 하는 값이다
   */
  const columns = useMemo(() => {
    const picked = selectedDate?.getDay() ?? -1;
    return Array.from({ length: DAYS_PER_WEEK }, (_, at) =>
      at === picked ? "2fr" : "1fr",
    ).join(" ");
  }, [selectedDate]);

  const shiftMonth = (delta: number) =>
    onMonthChange(new Date(month.getFullYear(), month.getMonth() + delta, 1));

  const todayKey = toLocalDate(new Date());
  const selectedKey = selectedDate ? toLocalDate(selectedDate) : null;

  return (
    <section className="flex flex-col gap-3">
      <header className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-ink">
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
            className="rounded-md border border-line px-2.5 py-1 text-xs text-ink-soft transition-colors hover:border-line hover:bg-accent-soft hover:text-accent"
          >
            오늘
          </button>
          <NavButton label="다음 달" onClick={() => shiftMonth(1)}>
            ›
          </NavButton>
        </div>
      </header>

      {/* 로딩 중에도 그리드를 그대로 둔다. 사라지면 이동 화살표가 튄다 */}
      <WarpBorder className="rounded-2xl p-3">
        {/*
          열 너비도 흐르게 바꾼다. fr 끼리는 사이값을 낼 수 있어
          1fr 에서 2fr 로 가는 길이 그려진다 — 칸 수가 그대로여야 성립한다
        */}
        <div
          className={`grid gap-1 transition-[grid-template-columns,opacity] duration-300 ease-out motion-reduce:transition-none ${
            loading ? "opacity-50" : ""
          }`}
          style={{ gridTemplateColumns: columns }}
          aria-busy={loading}
        >
          {WEEKDAYS.map((label, i) => (
            <div
              key={label}
              className={`pb-1 text-center text-xs font-semibold ${
                i === 0
                  ? "text-danger"
                  : i === 6
                    ? "text-accent"
                    : "text-ink-faint"
              }`}
            >
              {label}
            </div>
          ))}

          {days.map((date, at) => {
            const key = toLocalDate(date);
            return (
              <ScheduleCalendarCell
                key={key}
                date={date}
                day={byDate.get(key) ?? { lanes: [], hidden: 0 }}
                inCurrentMonth={date.getMonth() === month.getMonth()}
                isToday={key === todayKey}
                isSelected={key === selectedKey}
                selectedKey={selectedKey}
                onSelect={onSelectDate}
                expanded={Math.floor(at / DAYS_PER_WEEK) === openWeek}
                band={bandByDate.get(key) ?? { lanes: [], hidden: 0 }}
                entries={perDay.get(key) ?? []}
                onPick={onPickInstance}
                onToggleStatus={onToggleStatus}
              />
            );
          })}
        </div>
      </WarpBorder>
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
      className="rounded-md border border-line px-2.5 py-1 text-ink-soft transition-colors hover:border-line hover:bg-accent-soft hover:text-accent"
    >
      {children}
    </button>
  );
}

/** 기한을 뗀 사본. 띠를 그릴 때만 쓴다 */
function withoutDue(instance: ScheduleResponse): ScheduleResponse {
  return instance.dueOn === undefined ? instance : { ...instance, dueOn: undefined };
}
