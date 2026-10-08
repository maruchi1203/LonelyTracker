import type { SchedulePriority } from "../../../types/schedule";

/**
 * 우선순위를 왼쪽 선 하나로만 말한다.
 *
 * 달력 칸은 좁아 글자를 더 넣을 자리가 없고, 바탕색까지 우선순위에 내주면
 * 시작·기한을 가르는 색과 부딪힌다. 선 하나면 띠에도 줄에도 같은 자리에 붙는다
 */
export function priorityEdge(priority: SchedulePriority | undefined): string {
  if (priority === "MUST") return "border-l-danger";

  if (priority === "SHOULD") return "border-l-accent";

  return "border-l-ink-faint";
}
