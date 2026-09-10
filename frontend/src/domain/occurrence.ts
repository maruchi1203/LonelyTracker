import type { ScheduleResponse } from "../types/schedule";

/**
 * 반복 일정을 회차 하나로 줄인다
 * 달력이 한 달 치 회차를 전부 받아도 화면에는 지금 할 것 하나만 선다
 *
 * @param today "YYYY-MM-DD". 이 날짜 이후의 첫 회차를 고른다
 * @returns 1회성은 그대로, 반복은 하나씩만 남은 목록
 */
export function nearestOccurrences(
  instances: ScheduleResponse[],
  today: string,
): ScheduleResponse[] {
  const chosen = new Map<number, ScheduleResponse>();

  for (const instance of instances) {
    if (!instance.recurring || instance.instanceDate === undefined) continue;

    const standing = chosen.get(instance.id);
    if (standing === undefined || beats(instance, standing, today)) {
      chosen.set(instance.id, instance);
    }
  }

  return instances.filter(
    (i) =>
      !i.recurring ||
      i.instanceDate === undefined ||
      chosen.get(i.id) === i,
  );
}

/**
 * 오늘 이후의 첫 회차가 이긴다
 * 창이 전부 지난 날짜뿐이면 그중 가장 늦은 것이 오늘에 제일 가깝다
 */
function beats(
  candidate: ScheduleResponse,
  standing: ScheduleResponse,
  today: string,
): boolean {
  const mine = candidate.instanceDate ?? "";
  const theirs = standing.instanceDate ?? "";

  const mineAhead = mine >= today;
  const theirsAhead = theirs >= today;

  if (mineAhead !== theirsAhead) return mineAhead;
  return mineAhead ? mine < theirs : mine > theirs;
}
