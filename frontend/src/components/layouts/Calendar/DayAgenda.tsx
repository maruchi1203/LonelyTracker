import { instanceKey, sortByTimeOfDay } from "../../../domain/instance";
import type { ScheduleResponse } from "../../../types/schedule";
import { formatTime } from "../../../utils/datetime";
import { priorityEdge } from "./priorityEdge";

interface Props {
  instances: ScheduleResponse[];
  onPick: (instance: ScheduleResponse) => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
}

/** 펼친 주의 한 칸. 그 날 일정을 시각순으로 세운다 */
export default function DayAgenda({
  instances,
  onPick,
  onToggleStatus,
}: Props) {
  if (instances.length === 0) return null;

  return (
    // 줄만 누름을 받는다. 빈자리를 누르면 뒤에 깔린 날짜 고르기가 받게 둔다
    <ul className="pointer-events-auto flex list-none flex-col gap-1 p-0">
      {sortByTimeOfDay(instances).map((instance) => (
        <AgendaRow
          key={instanceKey(instance)}
          instance={instance}
          onPick={onPick}
          onToggleStatus={onToggleStatus}
        />
      ))}
    </ul>
  );
}

function AgendaRow({
  instance,
  onPick,
  onToggleStatus,
}: {
  instance: ScheduleResponse;
  onPick: (instance: ScheduleResponse) => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
}) {
  const done = instance.status === "DONE";
  const skipped = instance.status === "SKIPPED";

  return (
    <li
      className={`flex items-start gap-1.5 rounded-r border-l-2 bg-surface-soft/60 py-1 pr-1 pl-2 ${priorityEdge(
        instance.priority,
      )}`}
    >
      <input
        type="checkbox"
        checked={done}
        onChange={() => onToggleStatus(instance)}
        aria-label={`${instance.title} 완료 표시`}
        className="mt-0.5 size-3.5 shrink-0 accent-accent"
      />

      <button
        type="button"
        onClick={() => onPick(instance)}
        title={instance.title}
        className={`min-w-0 flex-1 truncate text-left text-sm leading-5 hover:underline ${
          done || skipped ? "text-ink-faint line-through" : "text-ink"
        }`}
      >
        {/* 시각이 제목보다 앞에 와야 세로로 훑을 때 차례가 읽힌다 */}
        {!instance.allDay && instance.startAt && (
          <span className="mr-1 tabular-nums text-ink-soft">
            {formatTime(new Date(instance.startAt))}
          </span>
        )}
        {instance.title}
      </button>
    </li>
  );
}
