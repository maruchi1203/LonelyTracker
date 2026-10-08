import { todayAgenda } from "../../../domain/dashboard";
import type {
  ScheduleListItem,
  ScheduleResponse,
} from "../../../types/schedule";
import { shortDate } from "../../../utils/datetime";

export interface AgendaProps {
  instances: ScheduleResponse[];
  items: ScheduleListItem[];
  today: string;
  busy: boolean;
  onToggleInstance: (instance: ScheduleResponse) => void;
  onCompleteItem: (item: ScheduleListItem) => void;
}

const CHECK =
  "size-4 shrink-0 cursor-pointer accent-ink disabled:cursor-not-allowed";

export default function TodayView({
  instances,
  items,
  today,
  busy,
  onToggleInstance,
  onCompleteItem,
}: AgendaProps) {
  const { overdue, scheduled, dueToday } = todayAgenda(instances, items, today);

  if (overdue.length + scheduled.length + dueToday.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-ink-faint">
        오늘은 잡힌 일이 없습니다
      </p>
    );
  }

  return (
    <ul className="flex list-none flex-col gap-1 p-0">
      {overdue.map((i) => (
        <li
          key={`overdue-${i.id}`}
          className="flex items-center gap-2 rounded-md bg-danger-soft px-2 py-1.5 text-sm"
        >
          <input
            type="checkbox"
            className={CHECK}
            disabled={busy}
            onChange={() => onCompleteItem(i)}
            aria-label={`${i.title} 완료`}
          />
          <span className="min-w-0 flex-1 truncate text-ink">
            {i.title}
          </span>
          <span className="shrink-0 text-xs text-danger">
            {shortDate(i.dueOn!)} 마감 지남
          </span>
        </li>
      ))}

      {scheduled.map((s) => {
        const done = s.status === "DONE";

        return (
          <li
            key={`${s.id}-${s.instanceDate ?? ""}`}
            className="flex items-center gap-2 px-2 py-1.5 text-sm"
          >
            <input
              type="checkbox"
              className={CHECK}
              checked={done}
              disabled={busy}
              onChange={() => onToggleInstance(s)}
              aria-label={`${s.title} 완료`}
            />
            <span className="w-11 shrink-0 text-xs text-ink-faint">
              {s.allDay || !s.startAt ? "종일" : s.startAt.slice(11, 16)}
            </span>
            <span
              className={`min-w-0 flex-1 truncate ${
                done || s.status === "SKIPPED"
                  ? "text-ink-faint line-through"
                  : "text-ink"
              }`}
            >
              {s.title}
            </span>
            {s.status === "SKIPPED" && (
              <span className="shrink-0 text-xs text-ink-faint">건너뜀</span>
            )}
            {s.recurring && (
              <span className="shrink-0 rounded-full bg-surface-soft px-1.5 py-0.5 text-[10px] text-ink-soft">
                반복
              </span>
            )}
          </li>
        );
      })}

      {dueToday.map((i) => (
        <li
          key={`due-${i.id}`}
          className="flex items-center gap-2 px-2 py-1.5 text-sm"
        >
          <input
            type="checkbox"
            className={CHECK}
            disabled={busy}
            onChange={() => onCompleteItem(i)}
            aria-label={`${i.title} 완료`}
          />
          <span className="w-11 shrink-0 text-xs text-warn">마감</span>
          <span className="min-w-0 flex-1 truncate text-ink">
            {i.title}
          </span>
        </li>
      ))}
    </ul>
  );
}
