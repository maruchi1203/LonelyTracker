import type { ScheduleListItem } from "../types/schedule";

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
 * 서버는 사용자가 세운 순서만 지킨다. 날짜로 줄 세우는 것은 여기서 한다
 */
export type ListSort = "manual" | "due";

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

  if (sort === "due") sortByDate(roots);
  return roots;
}

/** 형제 무리 안에서만 날짜순으로 세운다 */
function sortByDate(nodes: TreeNode[]): void {
  // 날짜가 없는 항목은 뒤로 보낸다. 같으면 서버가 준 순서가 그대로 남는다
  nodes.sort((a, b) => {
    const left = sortDateOf(a.item);
    const right = sortDateOf(b.item);
    if (left === right) return 0;
    if (left === undefined) return 1;
    if (right === undefined) return -1;
    return left < right ? -1 : 1;
  });
  nodes.forEach((node) => sortByDate(node.children));
}

/** 트리를 화면이 그릴 순서대로 편다 */
export function flatten(nodes: TreeNode[], depth = 0): FlatRow[] {
  return nodes.flatMap((node) => [
    { item: node.item, depth },
    ...flatten(node.children, depth + 1),
  ]);
}
