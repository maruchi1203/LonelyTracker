import { streakOf } from "../../../domain/habit";
import type { Habit } from "../../../types/habit";
import OrderButtons from "../OrderButtons";

const CELL = "h-7 min-w-7 flex-1 rounded-md border text-xs transition-colors";
const CELL_DONE = "border-accent bg-accent text-canvas";
const CELL_TODO = "border-line text-ink-faint hover:bg-accent-soft";

interface RowProps {
  habit: Habit;
  days: string[];
  today: string;
  /** 2분 법칙을 쓰기로 했는지. 끄면 신호 줄이 통째로 내려간다 */
  twoMinuteRule: boolean;
  /** 같은 카테고리에서 지금 보이는 습관들. 끝에서 단추를 잠그는 데 쓴다 */
  siblingIds: number[];
  onMove: (ids: number[]) => void;
  onToggle: (onDate: string) => void;
  onEdit: () => void;
  onArchive: () => void;
  onDelete: () => void;
}

export default function HabitRow({
  habit,
  days,
  today,
  twoMinuteRule,
  siblingIds,
  onMove,
  onToggle,
  onEdit,
  onArchive,
  onDelete,
}: RowProps) {
  const streak = streakOf(habit, today);

  /*
   * 언제·어디서·2분 행동은 따로 읽을 것이 아니라 한 문장으로 읽힌다.
   * "퇴근 후 · 거실 · 매트 깔기" 처럼 이어 두면 좁은 칸에서도 신호가 한눈에 들어온다
   */
  const cue = twoMinuteRule
    ? [habit.atTime, habit.place, habit.twoMinuteAction].filter(Boolean)
    : [];

  return (
    <li className={`list-none ${habit.archived ? "opacity-50" : ""}`}>
      <div className="flex flex-col gap-2 rounded-xl border border-line bg-surface p-3">
        <div className="flex items-start gap-2">
          <div className="flex min-w-0 flex-1 flex-col">
            <span className="truncate text-sm text-ink">{habit.title}</span>
            {cue.length > 0 && (
              <span className="truncate text-xs text-ink-faint">
                {cue.join(" · ")}
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
        </div>

        {/* 칸이 좁아질 수 있어 날짜가 폭을 나눠 갖는다 */}
        <div className="flex gap-1">
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

        {/* 차례 단추는 왼쪽에 둔다. 지우기 옆에 붙이면 잘못 누른다 */}
        <div className="flex items-center justify-between gap-1">
          <OrderButtons
            ids={siblingIds}
            id={habit.id}
            label={habit.title}
            onMove={onMove}
          />

          <div className="flex gap-1">
          <button
            type="button"
            onClick={onEdit}
            className="rounded-md border border-line px-2 py-1 text-xs text-ink-soft transition-colors hover:bg-surface-soft"
          >
            수정
          </button>
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
        </div>
      </div>
    </li>
  );
}
