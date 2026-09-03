import type { DayLanes, LaneSlot } from "../../../domain/calendarLanes";
import { instanceKey } from "../../../domain/instance";

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
      className={`flex min-h-28 flex-col gap-1 rounded-md border p-1.5 text-left transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100 ${
        isSelected
          ? "border-brand-500 bg-brand-50"
          : "border-slate-200 bg-white hover:border-brand-200 hover:bg-brand-50/40"
      } ${inCurrentMonth ? "" : "opacity-40"}`}
    >
      <span
        className={`self-start rounded-full px-1.5 text-xs font-semibold ${
          isToday
            ? "bg-brand-500 text-white"
            : inCurrentMonth
              ? "text-slate-600"
              : "text-slate-400"
        }`}
      >
        {date.getDate()}
      </span>

      <ul className="flex list-none flex-col gap-0.5 p-0">
        {day.lanes.map((slot, lane) =>
          slot ? (
            <Bar key={instanceKey(slot.instance)} slot={slot} />
          ) : (
            // 빈 레인도 자리를 차지해야 옆 칸의 띠와 높이가 맞는다
            <li key={`empty-${lane}`} className="h-4" aria-hidden />
          ),
        )}

        {day.hidden > 0 && (
          <li className="px-1 text-[11px] leading-4 text-slate-400">
            +{day.hidden}건
          </li>
        )}
      </ul>
    </button>
  );
}

function Bar({ slot }: { slot: LaneSlot }) {
  const { instance, isStart, isEnd } = slot;
  const done = instance.status === "DONE";

  const shape = [
    isStart ? "rounded-l-sm" : BLEED_LEFT,
    isEnd ? "rounded-r-sm" : BLEED_RIGHT,
  ].join(" ");

  return (
    <li
      // 칸이 좁으므로 한 줄로 자르고, 전체 제목은 title 속성으로 보여준다
      title={
        instance.postponeCount > 0
          ? `${instance.title} · ${instance.postponeCount}번 미룸`
          : instance.title
      }
      className={`${BAR} ${shape} ${
        done
          ? "bg-slate-100 text-slate-400 line-through"
          : "bg-brand-100 text-brand-800"
      }`}
    >
      {/* 제목은 띠가 시작하는 칸에만 적는다. 이어지는 칸은 띠만 보인다 */}
      {isStart && (
        <>
          {instance.postponeCount > 0 && (
            <span className="text-amber-600">↻ </span>
          )}
          {instance.title}
        </>
      )}
    </li>
  );
}
