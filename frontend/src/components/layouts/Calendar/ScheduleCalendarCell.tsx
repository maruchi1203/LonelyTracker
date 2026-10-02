import type { DayLanes, LaneSlot } from "../../../domain/calendarLanes";
import {
  instanceDateKeys,
  instanceKey,
  isMoved,
  type DayEntry,
} from "../../../domain/instance";
import type { ScheduleResponse } from "../../../types/schedule";
import DayAgenda from "./DayAgenda";
import { priorityEdge } from "./priorityEdge";

interface Props {
  date: Date;
  /** 이 날짜의 레인. 부모가 주 단위로 배정해 넘긴다 */
  day: DayLanes;
  /** 이번 달이 아닌 날(앞뒤로 채워진 칸)은 흐리게 표시한다 */
  inCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  /** 고른 날. 펼친 띠가 글자를 어디에 적을지 정하는 데 쓴다 */
  selectedKey: string | null;
  onSelect: (date: Date) => void;
  /** 고른 날이 든 주. 칸이 길어지고 하루짜리 일정이 줄로 선다 */
  expanded: boolean;
  /** 펼쳤을 때 위쪽에 깔 띠. 여러 날에 걸친 것만 들어 있다 */
  band: DayLanes;
  /** 펼쳤을 때 줄로 세울 것들. 하루짜리 일정과 그 날이 기한인 일정 */
  entries: DayEntry[];
  onPick: (instance: ScheduleResponse) => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
}

/**
 * 펼침과 접힘이 같은 뼈대를 쓴다.
 *
 * 둘을 다른 모양으로 그리면 React 가 칸을 갈아 끼워 높이가 튄다.
 * 뼈대가 같아야 min-height 가 이어져 늘고 주는 것이 보인다
 */
const SHELL =
  "relative flex flex-col gap-1.5 overflow-hidden rounded-md border p-1.5 text-left transition-[min-height] duration-300 ease-out motion-reduce:transition-none";

export default function ScheduleCalendarCell({
  date,
  day,
  inCurrentMonth,
  isToday,
  isSelected,
  selectedKey,
  onSelect,
  expanded,
  band,
  entries,
  onPick,
  onToggleStatus,
}: Props) {
  const total = day.lanes.filter(Boolean).length + day.hidden;
  const tone = isSelected
    ? "border-accent bg-accent-soft"
    : "border-line bg-surface";

  return (
    <div
      className={`${SHELL} ${expanded ? "min-h-80" : "min-h-28"} ${tone} ${
        inCurrentMonth ? "" : "opacity-40"
      }`}
    >
      {/*
        칸 전체가 날짜를 고르는 과녁이다. 단추를 글 뒤에 깔아 두는 까닭은,
        칸을 통째로 단추로 만들면 안에 든 체크상자와 줄이 단추 안의 단추가 되어서다.
        위에 얹은 글은 누름을 받지 않고, 눌러야 하는 것만 제 몫을 되찾는다
      */}
      <button
        type="button"
        onClick={() => onSelect(date)}
        aria-pressed={isSelected}
        aria-label={`${date.getMonth() + 1}월 ${date.getDate()}일, 일정 ${total}건`}
        className={`absolute inset-0 rounded-md transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line ${
          isSelected ? "" : "hover:bg-accent-soft/40"
        }`}
      />

      <div className="pointer-events-none relative flex min-h-0 flex-1 flex-col gap-1.5">
        <div className="flex items-center justify-between">
          <span
            className={`rounded-full px-1.5 font-semibold ${
              expanded ? "text-sm" : "text-xs"
            } ${
              isToday
                ? "bg-accent text-canvas"
                : inCurrentMonth
                  ? "text-ink-soft"
                  : "text-ink-faint"
            }`}
          >
            {date.getDate()}
          </span>

          {expanded && total > 0 && (
            <span className="text-xs text-ink-faint">{total}</span>
          )}
        </div>

        {expanded ? (
          <>
            {/*
              걸친 일정은 띠로 남는다. 빈 레인이 자리를 지켜 일곱 칸의 높이가 맞고,
              그래야 모서리를 이어 붙였을 때 하나로 보인다
            */}
            {band.lanes.length > 0 && (
              <ul className="flex list-none flex-col gap-0.5 p-0">
                {band.lanes.map((slot, lane) =>
                  slot ? (
                    <BandRow
                      key={`${instanceKey(slot.instance)}:${slot.kind}`}
                      slot={slot}
                      labelHere={labelsHere(slot, isSelected, selectedKey)}
                      onPick={onPick}
                      onToggleStatus={onToggleStatus}
                    />
                  ) : (
                    // 빈 레인도 같은 키로 자리를 지켜야 일곱 칸의 띠가 나란하다
                    <li key={`empty-${lane}`} className={BAND_H} aria-hidden />
                  ),
                )}
              </ul>
            )}

            <div className="min-h-0 flex-1 overflow-y-auto">
              <DayAgenda
                entries={entries}
                onPick={onPick}
                onToggleStatus={onToggleStatus}
              />
            </div>
          </>
        ) : (
          /*
            띠는 누름을 돌려받는다. title 툴팁이 hover 로 뜨는데 가려 두면,
            칸이 좁아 잘라 둔 제목을 확인할 길이 아예 없어진다.
            대신 띠를 눌러도 날짜가 골라지도록 이 겹이 그 몫을 대신 받는다
          */
          <ul
            onClick={() => onSelect(date)}
            className="pointer-events-auto flex list-none flex-col gap-0.5 p-0"
          >
            {day.lanes.map((slot, lane) =>
              slot ? (
                <Bar
                  key={`${instanceKey(slot.instance)}:${slot.kind}`}
                  slot={slot}
                  labelHere={slot.isStart}
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
        )}
      </div>
    </div>
  );
}

const BAR = "h-4 px-1 text-[11px] leading-4 truncate";

/** 펼친 칸의 띠 높이. 일곱 칸이 같아야 이어져 보인다 */
const BAND_H = "h-7";
/** 셀 패딩과 그리드 간격을 넘어가 옆 칸의 띠와 맞닿게 한다 */
const BLEED_LEFT = "-ml-2.5 pl-2.5";
const BLEED_RIGHT = "-mr-2.5 pr-2.5";

/**
 * 띠의 글자를 이 칸에 적을지.
 *
 * 펼친 주에서는 고른 날에 적는다 — 보고 있는 칸에 이름이 있어야 한다.
 * 그 띠가 고른 날까지 닿지 않으면 늘 하던 대로 띠가 시작하는 칸에 적는다
 */
function labelsHere(
  slot: LaneSlot,
  isSelected: boolean,
  selectedKey: string | null,
): boolean {
  const reaches =
    selectedKey !== null &&
    instanceDateKeys(slot.instance).includes(selectedKey);
  return reaches ? isSelected : slot.isStart;
}

/**
 * 띠의 모양과 색. 접힌 칸과 펼친 칸이 같은 규칙을 써야 같은 띠로 읽힌다.
 *
 * 시작은 왼쪽에, 기한은 오른쪽에 굵은 선을 둔다 — 색을 못 가려도 방향으로 갈린다.
 * 이어지는 칸에는 선을 두지 않는다. 띠 한가운데에 금이 그어져 두 개로 보인다.
 * 시작 쪽 선은 우선순위가 가져간다. 기한은 제 색을 지킨다 — 급한지보다 끝이라는 게 먼저다
 */
function skin(slot: LaneSlot): string {
  const { instance, kind, isStart, isEnd } = slot;
  const due = kind === "due";

  const shape = [
    isStart ? "rounded-l-sm" : BLEED_LEFT,
    isEnd ? "rounded-r-sm" : BLEED_RIGHT,
  ].join(" ");

  if (instance.status === "DONE") {
    return `${shape} bg-surface-soft text-ink-faint line-through`;
  }
  return due
    ? `${shape} bg-warn-soft text-warn ${isEnd ? "border-r-2 border-r-warn" : ""}`
    : `${shape} bg-accent-soft text-accent ${
        isStart ? `border-l-2 ${priorityEdge(instance.priority)}` : ""
      }`;
}

/**
 * 펼친 칸의 띠. 접힌 칸의 것보다 키우고, 글자가 서는 칸에만 체크상자와 누를 곳을 둔다.
 *
 * 체크상자를 걸친 날마다 두면 같은 일정에 상자가 여럿 생겨 어느 것을 눌러야 할지 모른다
 */
function BandRow({
  slot,
  labelHere,
  onPick,
  onToggleStatus,
}: {
  slot: LaneSlot;
  labelHere: boolean;
  onPick: (instance: ScheduleResponse) => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
}) {
  const { instance, kind } = slot;
  const done = instance.status === "DONE";
  const due = kind === "due";

  return (
    <li
      title={label(instance, due)}
      className={`${BAND_H} flex items-center gap-1.5 px-1 ${skin(slot)}`}
    >
      {labelHere && (
        <>
          <input
            type="checkbox"
            checked={done}
            onChange={() => onToggleStatus(instance)}
            aria-label={`${instance.title} 완료 표시`}
            className="pointer-events-auto size-3.5 shrink-0 accent-accent"
          />
          <button
            type="button"
            onClick={() => onPick(instance)}
            className="pointer-events-auto min-w-0 flex-1 truncate text-left text-sm leading-5 hover:underline"
          >
            {due && "~ "}
            {!due && isMoved(instance) && <span className="text-warn">↻ </span>}
            {instance.title}
          </button>
        </>
      )}
    </li>
  );
}

function Bar({ slot, labelHere }: { slot: LaneSlot; labelHere: boolean }) {
  const { instance, kind } = slot;
  const due = kind === "due";

  return (
    <li
      // 칸이 좁으므로 한 줄로 자르고, 전체 제목은 title 속성으로 보여준다
      title={label(instance, due)}
      className={`${BAR} ${skin(slot)}`}
    >
      {/* 제목은 한 칸에만 적는다. 이어지는 칸은 띠만 보인다 */}
      {labelHere && (
        <>
          {due && "~ "}
          {!due && isMoved(instance) && <span className="text-warn">↻ </span>}
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
