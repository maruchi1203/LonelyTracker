import { useState } from "react";
import { todayAgenda, weekOf, weekStats } from "../../domain/dashboard";
import type { ScheduleListItem, ScheduleResponse } from "../../types/schedule";
import DashboardCard, { SegmentToggle } from "./DashboardCard";

type View = "today" | "week";

interface Props {
  instances: ScheduleResponse[];
  items: ScheduleListItem[];
  today: string;
  busy: boolean;
  onToggleInstance: (instance: ScheduleResponse) => void;
  onCompleteItem: (item: ScheduleListItem) => void;
}

const CHECK = "size-4 shrink-0 cursor-pointer accent-brand-500 disabled:cursor-not-allowed";

/** 이번 주 보기에서 항목마다 보여줄 줄 수 */
const WEEK_ROWS = 5;

/** "2026-09-15" → "9/15" */
function shortDate(day: string): string {
  return `${Number(day.slice(5, 7))}/${Number(day.slice(8, 10))}`;
}

/** 오늘 할 일과 이번 주 요약을 한 칸에서 오간다 */
export default function AgendaPanel(props: Props) {
  const [view, setView] = useState<View>("today");

  return (
    <DashboardCard
      title={view === "today" ? "오늘" : "이번 주"}
      action={
        <SegmentToggle
          options={[
            { value: "today", label: "오늘" },
            { value: "week", label: "이번 주" },
          ]}
          value={view}
          onChange={setView}
        />
      }
    >
      {view === "today" ? <TodayView {...props} /> : <WeekView items={props.items} today={props.today} />}
    </DashboardCard>
  );
}

function TodayView({ instances, items, today, busy, onToggleInstance, onCompleteItem }: Props) {
  const { overdue, scheduled, dueToday } = todayAgenda(instances, items, today);

  if (overdue.length + scheduled.length + dueToday.length === 0) {
    return <p className="py-6 text-center text-sm text-slate-400">오늘은 잡힌 일이 없습니다.</p>;
  }

  return (
    <ul className="flex list-none flex-col gap-1 p-0">
      {overdue.map((i) => (
        <li key={`overdue-${i.id}`} className="flex items-center gap-2 rounded-md bg-rose-50 px-2 py-1.5 text-sm">
          <input
            type="checkbox"
            className={CHECK}
            disabled={busy}
            onChange={() => onCompleteItem(i)}
            aria-label={`${i.title} 완료`}
          />
          <span className="min-w-0 flex-1 truncate text-slate-800">{i.title}</span>
          <span className="shrink-0 text-xs text-rose-600">{shortDate(i.dueOn!)} 마감 지남</span>
        </li>
      ))}

      {scheduled.map((s) => {
        const done = s.status === "DONE";
        return (
          <li key={`${s.id}-${s.instanceDate ?? ""}`} className="flex items-center gap-2 px-2 py-1.5 text-sm">
            <input
              type="checkbox"
              className={CHECK}
              checked={done}
              disabled={busy}
              onChange={() => onToggleInstance(s)}
              aria-label={`${s.title} 완료`}
            />
            <span className="w-11 shrink-0 text-xs text-slate-400">
              {s.allDay || !s.startAt ? "종일" : s.startAt.slice(11, 16)}
            </span>
            <span
              className={`min-w-0 flex-1 truncate ${
                done || s.status === "SKIPPED" ? "text-slate-400 line-through" : "text-slate-800"
              }`}
            >
              {s.title}
            </span>
            {s.status === "SKIPPED" && <span className="shrink-0 text-xs text-slate-400">건너뜀</span>}
            {s.recurring && (
              <span className="shrink-0 rounded-full bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-500">반복</span>
            )}
          </li>
        );
      })}

      {dueToday.map((i) => (
        <li key={`due-${i.id}`} className="flex items-center gap-2 px-2 py-1.5 text-sm">
          <input
            type="checkbox"
            className={CHECK}
            disabled={busy}
            onChange={() => onCompleteItem(i)}
            aria-label={`${i.title} 완료`}
          />
          <span className="w-11 shrink-0 text-xs text-amber-600">마감</span>
          <span className="min-w-0 flex-1 truncate text-slate-800">{i.title}</span>
        </li>
      ))}
    </ul>
  );
}

function WeekView({ items, today }: { items: ScheduleListItem[]; today: string }) {
  const { must, should, dueSoon } = weekStats(items, today);
  const week = weekOf(today);

  return (
    <div className="flex flex-col gap-4">
      <p className="text-xs text-slate-400">
        {shortDate(week.from)} ~ {shortDate(week.to)}
      </p>
      <ItemSection title="필수" empty="끝내지 않은 필수가 없습니다." items={must} tone="text-rose-600" />
      <ItemSection title="권장" empty="끝내지 않은 권장이 없습니다." items={should} tone="text-amber-600" />
      <ItemSection title="7일 안 마감" empty="없습니다." items={dueSoon} tone="text-slate-700" />
    </div>
  );
}

interface SectionProps {
  title: string;
  empty: string;
  items: ScheduleListItem[];
  /** 개수 숫자의 색 */
  tone: string;
}

function ItemSection({ title, empty, items, tone }: SectionProps) {
  const rest = items.length - WEEK_ROWS;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between text-sm">
        <span className="text-slate-600">{title}</span>
        <span className={`font-semibold ${items.length > 0 ? tone : "text-slate-400"}`}>{items.length}</span>
      </div>

      {items.length === 0 ? (
        <p className="text-xs text-slate-400">{empty}</p>
      ) : (
        <ul className="flex list-none flex-col gap-0.5 p-0">
          {items.slice(0, WEEK_ROWS).map((i) => (
            <li key={i.id} className="flex items-center gap-2 text-sm">
              <span className="w-11 shrink-0 text-xs text-slate-400">{i.dueOn ? shortDate(i.dueOn) : "—"}</span>
              <span className="min-w-0 flex-1 truncate text-slate-800">{i.title}</span>
            </li>
          ))}
          {rest > 0 && <li className="text-xs text-slate-400">외 {rest}개</li>}
        </ul>
      )}
    </div>
  );
}
