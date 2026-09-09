import type { ScheduleListItem } from "../types/schedule";
import { selfAndDescendantIds } from "./scheduleTree";

/**
 * 행 하나에 놓았을 때의 뜻
 * before/after 는 그 행의 형제가 되고, inside 는 그 행의 자식이 된다
 */
export type DropIntent = "before" | "inside" | "after";

/** 자식으로 넣는 가운데 띠의 폭. 행의 절반을 준다 */
const INSIDE_BAND = 0.5;

/**
 * 커서가 행의 어디에 있는지로 뜻을 가른다
 * 가운데가 넓어 계층 넣기가 쉽고, 순서만 바꿀 때는 위아래 끝을 노린다
 *
 * @param ratio 행 위쪽 끝이 0, 아래쪽 끝이 1
 */
export function dropIntentAt(ratio: number): DropIntent {
  const edge = (1 - INSIDE_BAND) / 2;
  if (ratio < edge) return "before";
  if (ratio > 1 - edge) return "after";
  return "inside";
}

/** 옮긴 결과. 서버에 보낼 무리 하나를 담는다 */
export interface DropPlan {
  parentId: number | null;
  ids: number[];
}

/**
 * 끌어다 놓은 결과로 다시 세울 무리를 만든다
 * 서버는 무리의 최종 구성원 전부를 요구하므로 밖에서 들어온 것까지 한 줄에 담는다
 *
 * @returns 옮길 필요가 없거나 놓을 수 없는 자리면 null
 */
export function planDrop(
  items: ScheduleListItem[],
  draggedId: number,
  targetId: number,
  intent: DropIntent,
): DropPlan | null {
  const dragged = items.find((i) => i.id === draggedId);
  const target = items.find((i) => i.id === targetId);
  if (!dragged || !target || draggedId === targetId) return null;

  // 자기 자손 밑으로 들어가면 그 가지가 통째로 트리에서 떨어져 나간다
  if (selfAndDescendantIds(items, draggedId).has(targetId)) return null;

  const parentId = intent === "inside" ? target.id : (target.parentId ?? null);

  // 습관은 계층에 끼지 않는다. 서버도 같은 것을 막는다
  if (parentId !== null && (dragged.recurring || parentIsHabit(items, parentId))) {
    return null;
  }
  if (parentId !== null && depthOf(items, parentId) >= 2) return null;

  // items 는 서버가 준 차례라 저장된 순서 그대로다
  const ids = items
    .filter((i) => (i.parentId ?? null) === parentId && i.id !== draggedId)
    .map((i) => i.id);

  if (intent === "inside") ids.push(draggedId);
  else {
    const at = ids.indexOf(target.id);
    ids.splice(intent === "before" ? at : at + 1, 0, draggedId);
  }

  return { parentId, ids };
}

/**
 * 최상위 무리의 맨 끝으로 보낸다
 * 마지막 행이 깊은 곳에 있으면 그 아래에는 최상위 자리가 없어 따로 둔다
 *
 * @returns 이미 최상위 끝자리에 있으면 null
 */
export function planDropAtEnd(
  items: ScheduleListItem[],
  draggedId: number,
): DropPlan | null {
  if (!items.some((i) => i.id === draggedId)) return null;

  const top = items.filter((i) => (i.parentId ?? null) === null);
  if (top[top.length - 1]?.id === draggedId) return null;

  const ids = top.filter((i) => i.id !== draggedId).map((i) => i.id);
  ids.push(draggedId);

  return { parentId: null, ids };
}

function parentIsHabit(items: ScheduleListItem[], parentId: number): boolean {
  return items.find((i) => i.id === parentId)?.recurring === true;
}

/** 최상위까지의 거리. 최상위 자신은 0이다 */
function depthOf(items: ScheduleListItem[], id: number): number {
  let depth = 0;
  let cursor = items.find((i) => i.id === id)?.parentId;

  // 깨진 데이터로 서로를 가리켜도 멈추도록 상한을 둔다
  while (cursor !== undefined && depth < 3) {
    depth++;
    cursor = items.find((i) => i.id === cursor)?.parentId;
  }
  return depth;
}
