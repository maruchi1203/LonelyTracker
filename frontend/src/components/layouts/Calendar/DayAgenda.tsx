import { instanceKey, type DayEntry } from "../../../domain/instance";
import type { ScheduleResponse } from "../../../types/schedule";
import { formatTime } from "../../../utils/datetime";
import { priorityEdge } from "./priorityEdge";

interface Props {
  entries: DayEntry[];
  onPick: (instance: ScheduleResponse) => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
}

/** 펼친 주의 한 칸. 하루 안에 끝나는 일정과 그 날이 기한인 일정을 세운다 */
export default function DayAgenda({ entries, onPick, onToggleStatus }: Props) {
  if (entries.length === 0) return null;

  return (
    // 줄만 누름을 받는다. 빈자리를 누르면 뒤에 깔린 날짜 고르기가 받게 둔다
    <ul className="pointer-events-auto flex list-none flex-col gap-1 p-0">
      {entries.map((entry) => (
        <AgendaRow
          key={`${instanceKey(entry.instance)}:${entry.kind}`}
          entry={entry}
          onPick={onPick}
          onToggleStatus={onToggleStatus}
        />
      ))}
    </ul>
  );
}

function AgendaRow({
  entry,
  onPick,
  onToggleStatus,
}: {
  entry: DayEntry;
  onPick: (instance: ScheduleResponse) => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
}) {
  const { instance, kind } = entry;
  const done = instance.status === "DONE";
  const skipped = instance.status === "SKIPPED";
  const due = kind === "due";

  return (
    <li
      className={`flex items-start gap-1.5 rounded-r border-l-2 py-1 pr-1 pl-2 ${
        due
          ? "border-l-warn bg-warn-soft/50"
          : `bg-surface-soft/60 ${priorityEdge(instance.priority)}`
      }`}
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
        title={
          due ? `${instance.title} · ${instance.dueOn} 까지` : instance.title
        }
        className={`min-w-0 flex-1 truncate text-left text-sm leading-5 hover:underline ${
          done || skipped ? "text-ink-faint line-through" : "text-ink"
        }`}
      >
        {/* 기한은 시작과 섞이면 안 된다. 색만으로는 못 가리므로 글자로도 밝힌다 */}
        {due && <span className="mr-0.5 text-warn">~</span>}
        {instance.title}
      </button>

      {/* 오른쪽 끝에 세운다. 제목 길이가 제각각이라 앞에 두면 자리가 들쭉날쭉하다 */}
      {due ? (
        <span className="shrink-0 text-xs leading-5 text-warn">마감</span>
      ) : (
        !instance.allDay &&
        instance.startAt && (
          <span className="shrink-0 text-xs leading-5 tabular-nums text-ink-soft">
            {formatTime(new Date(instance.startAt))}
          </span>
        )
      )}
    </li>
  );
}
