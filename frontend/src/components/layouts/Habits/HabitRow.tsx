import { streakOf } from "../../../domain/habit";
import type { Habit } from "../../../types/habit";

const CELL = "size-7 rounded-md border text-xs transition-colors";
const CELL_DONE = "border-accent bg-accent text-canvas";
const CELL_TODO = "border-line text-ink-faint hover:bg-accent-soft";

interface RowProps {
  habit: Habit;
  days: string[];
  today: string;
  onToggle: (onDate: string) => void;
  onArchive: () => void;
  onDelete: () => void;
}

export default function HabitRow({
  habit,
  days,
  today,
  onToggle,
  onArchive,
  onDelete,
}: RowProps) {
  const streak = streakOf(habit, today);

  return (
    <li
      className={`flex flex-wrap items-center gap-3 rounded-md px-1 py-2 ${
        habit.archived ? "opacity-50" : ""
      }`}
    >
      <div className="flex min-w-0 flex-1 basis-48 flex-col">
        <span className="truncate text-sm text-ink">{habit.title}</span>
        {habit.twoMinuteAction && (
          <span className="truncate text-xs text-ink-faint">
            2분: {habit.twoMinuteAction}
          </span>
        )}
      </div>

      {streak > 0 && (
        <span
          title="오늘까지 이어 온 날"
          className="shrink-0 rounded-full bg-accent-soft px-2 py-0.5 text-xs text-accent"
        >
          {streak}일째
        </span>
      )}

      <div className="flex shrink-0 gap-1">
        {days.map((day) => {
          const done = habit.doneDates.includes(day);
          return (
            <button
              key={day}
              type="button"
              onClick={() => onToggle(day)}
              aria-pressed={done}
              aria-label={`${habit.title} ${day}`}
              title={day}
              className={`${CELL} ${done ? CELL_DONE : CELL_TODO} ${
                day === today ? "ring-2 ring-line" : ""
              }`}
            >
              {Number(day.slice(8))}
            </button>
          );
        })}
      </div>

      <div className="flex shrink-0 gap-1">
        <button
          type="button"
          onClick={onArchive}
          className="rounded-md border border-line px-2 py-1 text-xs text-ink-soft transition-colors hover:bg-surface-soft"
        >
          {habit.archived ? "다시" : "그만"}
        </button>
        <button
          type="button"
          onClick={onDelete}
          className="rounded-md border border-transparent px-2 py-1 text-xs text-danger transition-colors hover:border-danger hover:bg-danger-soft"
        >
          삭제
        </button>
      </div>
    </li>
  );
}
