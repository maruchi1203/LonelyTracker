import type { ScheduleListItem } from "../types/schedule";
import { buildTree, flatten, selfAndDescendantIds } from "./scheduleTree";

/**
 * 행 하나에 놓았을 때의 뜻
 * before/after 는 그 행 앞뒤의 틈이고, inside 는 그 행의 막내 자식이 된다
 */
export type DropIntent = "before" | "inside" | "after";

/** 계층은 3단까지다. 최상위를 0으로 센다 */
const DEEPEST = 2;

/** 자식으로 넣는 가운데 띠의 폭. 행의 절반을 준다 */
const INSIDE_BAND = 0.5;

/** 들여쓰기 한 칸. 화면의 INDENT 와 같아야 손과 눈이 맞는다 */
export const INDENT_PX = 32;

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

/**
 * 커서의 가로 자리로 몇 단에 설지 읽는다
 * 왼쪽 끝에 두면 최상위, 한 칸씩 오른쪽으로 갈수록 한 단씩 깊어진다
 *
 * @param offsetX 행 왼쪽 끝에서 떨어진 거리
 */
export function dropLevelAt(offsetX: number): number {
  const level = Math.floor(offsetX / INDENT_PX);
  return Math.min(Math.max(level, 0), DEEPEST);
}

/** 옮긴 결과. 서버에 보낼 무리 하나를 담는다 */
export interface DropPlan {
  parentId: number | null;
  ids: number[];
  /** 실제로 서게 되는 단. 커서를 오른쪽 끝까지 밀어도 여기까지다 */
  level: number;
}

/** 설 자리. afterId 가 null 이면 그 무리의 맨 앞이다 */
interface Spot {
  parentId: number | null;
  afterId: number | null;
  level: number;
}

/**
 * 끌어다 놓은 결과로 다시 세울 무리를 만든다
 * 서버는 무리의 최종 구성원 전부를 요구하므로 밖에서 들어온 것까지 한 줄에 담는다
 *
 * @param level before/after 일 때 몇 단에 설지. inside 는 대상의 자식이라 안 쓴다
 * @returns 옮길 필요가 없거나 놓을 수 없는 자리면 null
 */
export function planDrop(
  items: ScheduleListItem[],
  draggedId: number,
  targetId: number,
  intent: DropIntent,
  level: number,
): DropPlan | null {
  const dragged = items.find((i) => i.id === draggedId);
  const target = items.find((i) => i.id === targetId);
  if (!dragged || !target || draggedId === targetId) return null;

  // 자기 자손 밑으로 들어가면 그 가지가 통째로 트리에서 떨어져 나간다
  const moving = selfAndDescendantIds(items, draggedId);
  if (moving.has(targetId)) return null;

  // 습관은 남의 밑에 서지 않아 오른쪽으로 밀어도 최상위에 머문다
  const wanted = dragged.recurring ? 0 : level;

  const spot =
    intent === "inside"
      ? lastChildSpot(items, target.id, draggedId)
      : gapSpot(items, moving, target.id, intent, wanted);
  if (spot === null) return null;

  const { parentId } = spot;
  if (parentId !== null) {
    // 습관은 계층에 끼지 않는다. 서버도 같은 것을 막는다
    if (dragged.recurring || isHabit(items, parentId)) return null;
    if (depthOf(items, parentId) >= DEEPEST) return null;
  }

  // items 는 서버가 준 차례라 저장된 순서 그대로다
  const ids = items
    .filter((i) => (i.parentId ?? null) === parentId && i.id !== draggedId)
    .map((i) => i.id);

  const at = spot.afterId === null ? 0 : ids.indexOf(spot.afterId) + 1;
  ids.splice(at, 0, draggedId);

  return { parentId, ids, level: spot.level };
}

/** 그 행의 자식 무리 맨 끝 */
function lastChildSpot(
  items: ScheduleListItem[],
  targetId: number,
  draggedId: number,
): Spot {
  const children = items.filter(
    (i) => (i.parentId ?? null) === targetId && i.id !== draggedId,
  );
  return {
    parentId: targetId,
    afterId: children.at(-1)?.id ?? null,
    level: depthOf(items, targetId) + 1,
  };
}

/**
 * 행 사이의 틈에서 그 단의 자리를 찾는다
 * 틈 바로 위 행에서 조상을 거슬러 올라가면 단마다 자리가 하나씩 나온다
 */
function gapSpot(
  items: ScheduleListItem[],
  moving: Set<number>,
  targetId: number,
  intent: "before" | "after",
  level: number,
): Spot | null {
  // 끌고 있는 가지는 없는 셈 친다. 자기가 자기 위 행이 되면 자기 밑으로 들어간다
  const rows = flatten(buildTree(items)).filter((r) => !moving.has(r.item.id));
  const at = rows.findIndex((r) => r.item.id === targetId);
  if (at < 0) return null;

  const above = intent === "before" ? rows[at - 1] : rows[at];
  if (above === undefined) return { parentId: null, afterId: null, level: 0 };

  // 오른쪽으로 아무리 밀어도 바로 위 행보다 한 단 깊은 곳까지다
  let wanted = Math.min(level, above.depth + 1, DEEPEST);

  // 습관은 자식을 거느리지 못해 그 밑의 단은 처음부터 없는 자리다
  if (wanted === above.depth + 1 && above.item.recurring) wanted--;

  if (wanted === above.depth + 1) {
    return { parentId: above.item.id, afterId: null, level: wanted };
  }

  // 그 단까지 거슬러 올라간 조상 바로 뒤에 선다
  let anchor = above.item;
  for (let depth = above.depth; depth > wanted; depth--) {
    const parent = items.find((i) => i.id === anchor.parentId);
    if (parent === undefined) return null;
    anchor = parent;
  }
  return { parentId: anchor.parentId ?? null, afterId: anchor.id, level: wanted };
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

  return { parentId: null, ids, level: 0 };
}

function isHabit(items: ScheduleListItem[], id: number): boolean {
  return items.find((i) => i.id === id)?.recurring === true;
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
