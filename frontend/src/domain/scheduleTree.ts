import type { ScheduleListItem, SchedulePriority } from "../types/schedule";

/** 리스트 한 줄과 그 밑에 딸린 것들 */
export interface TreeNode {
  item: ScheduleListItem;
  children: TreeNode[];
}

/** 트리를 다시 평평하게 편 한 줄. depth 는 들여쓰기에 쓴다 */
export interface FlatRow {
  item: ScheduleListItem;
  depth: number;
}

/**
 * 늘어놓는 기준
 * 서버는 사용자가 세운 순서만 지킨다. 날짜와 우선순위로 줄 세우는 것은 여기서 한다
 */
export type ListSort = "manual" | "due" | "priority";

/** 값이 없으면 COULD 자리에 둔다. 저장은 구분하고 표시만 합친다 */
const PRIORITY_RANK: Record<SchedulePriority, number> = {
  MUST: 0,
  SHOULD: 1,
  COULD: 2,
  WONT: 3,
};

function rankOf(item: ScheduleListItem): number {
  return PRIORITY_RANK[item.priority ?? "COULD"];
}

/**
 * 정렬에 쓰는 날짜
 * 리스트가 묻는 것은 "언제까지"라 기한이 시작일시를 이긴다
 *
 * @returns "YYYY-MM-DD". 둘 다 없으면 undefined
 */
export function sortDateOf(item: ScheduleListItem): string | undefined {
  return item.dueOn ?? item.startAt?.slice(0, 10);
}

/**
 * 평평한 목록을 부모-자식으로 묶는다
 * 부모를 못 찾은 항목은 최상위로 올린다. 화면에서 항목이 사라지는 것이 가장 나쁘다
 *
 * @param sort due 면 형제끼리만 날짜순으로 바꾼다. 계층을 넘어 섞지 않는다
 */
export function buildTree(
  items: ScheduleListItem[],
  sort: ListSort = "manual",
): TreeNode[] {
  const byId = new Map<number, TreeNode>(
    items.map((item) => [item.id, { item, children: [] }]),
  );

  const roots: TreeNode[] = [];
  for (const node of byId.values()) {
    const parent =
      node.item.parentId === undefined
        ? undefined
        : byId.get(node.item.parentId);

    // 자기 자신을 부모로 가리키는 값이 들어와도 무한히 돌지 않게 막는다
    if (parent && parent !== node) parent.children.push(node);
    else roots.push(node);
  }

  if (sort !== "manual") sortNodes(roots, sort);
  return roots;
}

/** 형제 무리 안에서만 세운다. 계층을 넘어 섞으면 자식이 부모보다 앞에 선다 */
function sortNodes(nodes: TreeNode[], sort: Exclude<ListSort, "manual">): void {
  nodes.sort((a, b) => {
    // 우선순위가 같으면 마감이 빠른 것부터. 같으면 서버가 준 순서가 남는다
    if (sort === "priority") {
      const gap = rankOf(a.item) - rankOf(b.item);
      if (gap !== 0) return gap;
    }
    return compareByDate(a, b);
  });
  nodes.forEach((node) => sortNodes(node.children, sort));
}

/** 날짜가 없는 항목은 뒤로 보낸다 */
function compareByDate(a: TreeNode, b: TreeNode): number {
  const left = sortDateOf(a.item);
  const right = sortDateOf(b.item);
  if (left === right) return 0;
  if (left === undefined) return 1;
  if (right === undefined) return -1;
  return left < right ? -1 : 1;
}

/**
 * 그 항목과 그 밑에 딸린 것 전부의 id
 * 수정 폼의 상위 후보에서 빼는 데 쓴다. 자기 자손을 부모로 삼으면 순환이 된다
 */
export function selfAndDescendantIds(
  items: ScheduleListItem[],
  id: number,
): Set<number> {
  const childrenOf = new Map<number, number[]>();
  for (const item of items) {
    if (item.parentId === undefined) continue;
    childrenOf.set(item.parentId, [
      ...(childrenOf.get(item.parentId) ?? []),
      item.id,
    ]);
  }

  const found = new Set<number>([id]);
  const pending = [id];

  // 이미 담은 것은 다시 펼치지 않는다. 서로를 가리켜도 두 번째 만남에서 멈춘다
  for (let cursor = pending.pop(); cursor !== undefined; cursor = pending.pop()) {
    for (const child of childrenOf.get(cursor) ?? []) {
      if (found.has(child)) continue;
      found.add(child);
      pending.push(child);
    }
  }
  return found;
}

/** 트리를 화면이 그릴 순서대로 편다 */
export function flatten(nodes: TreeNode[], depth = 0): FlatRow[] {
  return nodes.flatMap((node) => [
    { item: node.item, depth },
    ...flatten(node.children, depth + 1),
  ]);
}
