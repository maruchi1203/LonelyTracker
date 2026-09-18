import type { Habit } from "../types/habit";
import type {
  AiProvider,
  ProviderUsage,
  ScheduleListItem,
  ScheduleResponse,
} from "../types/schedule";
import { toLocalDate } from "../utils/datetime";
import { groupByCategory, streakOf } from "./habit";
import { sameBaseUrl } from "../constants/aiPresets";

/** "YYYY-MM-DD" 두 개로 닫힌 기간 */
export interface DayRange {
  from: string;
  to: string;
}

function shift(day: string, days: number): string {
  const d = new Date(`${day}T00:00:00`);
  d.setDate(d.getDate() + days);
  return toLocalDate(d);
}

/** 그날이 속한 주. 월요일에 시작한다 */
export function weekOf(today: string): DayRange {
  const d = new Date(`${today}T00:00:00`);
  const sinceMonday = (d.getDay() + 6) % 7;
  const from = shift(today, -sinceMonday);
  return { from, to: shift(from, 6) };
}

/** 끝내지 않은 1회성 항목. 안 하기로 한 것과 반복은 뺀다 */
function isOpen(item: ScheduleListItem): boolean {
  return !item.completedAt && item.priority !== "WONT" && !item.recurring;
}

/** 마감이 가까운 것이 앞, 마감이 없는 것은 원래 순서대로 뒤 */
function byDue(items: ScheduleListItem[]): ScheduleListItem[] {
  return [...items].sort((a, b) => {
    if (a.dueOn === b.dueOn) return 0;
    if (a.dueOn === undefined) return 1;
    if (b.dueOn === undefined) return -1;
    return a.dueOn.localeCompare(b.dueOn);
  });
}

/**
 * 회차가 차지하는 날들의 처음과 끝
 * 자정에 끝나는 기간은 그 전날까지로 본다
 */
function daysOf(s: ScheduleResponse): DayRange | undefined {
  if (!s.startAt) return undefined;
  const from = s.startAt.slice(0, 10);
  if (!s.endAt) return { from, to: from };

  let to = s.endAt.slice(0, 10);
  if (to > from && s.endAt.slice(11) === "00:00:00") to = shift(to, -1);
  return { from, to };
}

export interface TodayAgenda {
  /** 마감이 지났는데 안 끝낸 것. 오래된 것이 앞 */
  overdue: ScheduleListItem[];
  /** 오늘에 걸친 회차. 종일이 앞, 나머지는 시작 시각 순 */
  scheduled: ScheduleResponse[];
  /** 오늘 마감인데 오늘 회차로는 안 잡힌 것 */
  dueToday: ScheduleListItem[];
}

export function todayAgenda(
  instances: ScheduleResponse[],
  items: ScheduleListItem[],
  today: string,
): TodayAgenda {
  const scheduled = instances
    .filter((s) => {
      const days = daysOf(s);
      return days !== undefined && days.from <= today && today <= days.to;
    })
    .sort((a, b) => {
      if (a.allDay !== b.allDay) return a.allDay ? -1 : 1;
      return (a.startAt ?? "").localeCompare(b.startAt ?? "");
    });

  // 오늘 회차에 이미 올라온 일정은 마감 줄에 두 번 세우지 않는다
  const scheduledIds = new Set(scheduled.map((s) => s.id));

  return {
    overdue: byDue(items.filter((i) => isOpen(i) && i.dueOn !== undefined && i.dueOn < today)),
    scheduled,
    dueToday: items.filter(
      (i) => isOpen(i) && i.dueOn === today && !scheduledIds.has(i.id),
    ),
  };
}

export interface WeekStats {
  /** 끝내지 않은 필수. 마감이 가까운 것이 앞 */
  must: ScheduleListItem[];
  /** 끝내지 않은 권장. 마감이 가까운 것이 앞 */
  should: ScheduleListItem[];
  /** 오늘부터 7일 안에 마감인 것. 가까운 것이 앞 */
  dueSoon: ScheduleListItem[];
}

export function weekStats(items: ScheduleListItem[], today: string): WeekStats {
  const open = items.filter(isOpen);
  const soonEnd = shift(today, 6);

  return {
    must: byDue(open.filter((i) => i.priority === "MUST")),
    should: byDue(open.filter((i) => i.priority === "SHOULD")),
    dueSoon: byDue(
      open.filter((i) => i.dueOn !== undefined && today <= i.dueOn && i.dueOn <= soonEnd),
    ),
  };
}

export interface HabitSummary {
  doneToday: number;
  /** 그만두지 않은 습관 수 */
  total: number;
  /** 연속일이 긴 순으로 셋까지 */
  topStreaks: { habit: Habit; streak: number }[];
  /** 아직 습관이 없는 갈래 이름 */
  emptyCategories: string[];
}

export function habitSummary(habits: Habit[], today: string): HabitSummary {
  const active = habits.filter((h) => !h.archived);

  return {
    doneToday: active.filter((h) => h.doneDates.includes(today)).length,
    total: active.length,
    topStreaks: active
      .map((habit) => ({ habit, streak: streakOf(habit, today) }))
      .filter((s) => s.streak > 0)
      .sort((a, b) => b.streak - a.streak)
      .slice(0, 3),
    emptyCategories: groupByCategory(active)
      .filter((g) => g.habits.length === 0)
      .map((g) => g.label),
  };
}

/** 한도의 몇 할을 썼는지. 한도를 정하지 않은 제공자면 null */
export function limitRatio(
  usage: ProviderUsage,
  providers: AiProvider[],
): number | null {
  const limit = providers.find((p) =>
    sameBaseUrl(p.baseUrl, usage.baseUrl),
  )?.monthlyTokenLimit;
  if (!limit) return null;

  return Math.min((usage.inputTokens + usage.outputTokens) / limit, 1);
}
