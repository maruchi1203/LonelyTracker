import type { DayLanes, LaneSlot } from "../../../domain/calendarLanes";
import { instanceKey, isMoved } from "../../../domain/instance";

interface Props {
  date: Date;
  /** 이 날짜의 레인. 부모가 주 단위로 배정해 넘긴다 */
  day: DayLanes;
  /** 이번 달이 아닌 날(앞뒤로 채워진 칸)은 흐리게 표시한다 */
  inCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  onSelect: (date: Date) => void;
}

const BAR = "h-4 px-1 text-[11px] leading-4 truncate";
/** 셀 패딩과 그리드 간격을 넘어가 옆 칸의 띠와 맞닿게 한다 */
const BLEED_LEFT = "-ml-2.5 pl-2.5";
const BLEED_RIGHT = "-mr-2.5 pr-2.5";

export default function ScheduleCalendarCell({
  date,
  day,
  inCurrentMonth,
  isToday,
  isSelected,
  onSelect,
}: Props) {
  const count = day.lanes.filter(Boolean).length + day.hidden;

  return (
    <button
      type="button"
      onClick={() => onSelect(date)}
      aria-label={`${date.getMonth() + 1}월 ${date.getDate()}일, 일정 ${count}건`}
      aria-pressed={isSelected}
      className={`flex min-h-28 flex-col gap-1 rounded-md border p-1.5 text-left transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line ${
        isSelected
          ? "border-accent bg-accent-soft"
          : "border-line bg-surface hover:border-line hover:bg-accent-soft/40"
      } ${inCurrentMonth ? "" : "opacity-40"}`}
    >
      <span
        className={`self-start rounded-full px-1.5 text-xs font-semibold ${
          isToday
            ? "bg-accent text-canvas"
            : inCurrentMonth
              ? "text-ink-soft"
              : "text-ink-faint"
        }`}
      >
        {date.getDate()}
      </span>

      <ul className="flex list-none flex-col gap-0.5 p-0">
        {day.lanes.map((slot, lane) =>
          slot ? (
            <Bar
              key={`${instanceKey(slot.instance)}:${slot.kind}`}
              slot={slot}
            />
          ) : (
            // 빈 레인도 자리를 차지해야 옆 칸의 띠와 높이가 맞는다
            <li key={`empty-${lane}`} className="h-4" aria-hidden />
          ),
        )}

        {day.hidden > 0 && (
          <li className="px-1 text-[11px] leading-4 text-ink-faint">
            +{day.hidden}건
          </li>
        )}
      </ul>
    </button>
  );
}

function Bar({ slot }: { slot: LaneSlot }) {
  const { instance, kind, isStart, isEnd } = slot;
  const done = instance.status === "DONE";
  const due = kind === "due";

  const shape = [
    isStart ? "rounded-l-sm" : BLEED_LEFT,
    isEnd ? "rounded-r-sm" : BLEED_RIGHT,
  ].join(" ");

  // 시작은 왼쪽에, 기한은 오른쪽에 굵은 선을 둔다. 색을 못 가려도 방향으로 갈린다.
  // 이어지는 칸에는 선을 두지 않는다. 띠 한가운데에 금이 그어져 두 개로 보인다
  const tone = due
    ? `bg-warn-soft text-warn ${isEnd ? "border-r-2 border-r-warn" : ""}`
    : `bg-accent-soft text-accent ${isStart ? "border-l-2 border-l-accent" : ""}`;

  return (
    <li
      // 칸이 좁으므로 한 줄로 자르고, 전체 제목은 title 속성으로 보여준다
      title={label(instance, due)}
      className={`${BAR} ${shape} ${
        done ? "bg-surface-soft text-ink-faint line-through" : tone
      }`}
    >
      {/* 제목은 띠가 시작하는 칸에만 적는다. 이어지는 칸은 띠만 보인다 */}
      {isStart && (
        <>
          {due && "~ "}
          {!due && isMoved(instance) && (
            <span className="text-warn">↻ </span>
          )}
          {instance.title}
        </>
      )}
    </li>
  );
}

/** 띠에 올려두는 설명. 시작인지 기한인지부터 밝힌다 */
function label(instance: LaneSlot["instance"], due: boolean): string {
  if (due) return `${instance.title} · ${instance.dueOn} 까지`;
  return isMoved(instance)
    ? `${instance.title} · 원래 ${instance.instanceDate} 예정`
    : instance.title;
}
