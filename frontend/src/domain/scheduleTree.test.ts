import { describe, expect, it } from 'vitest'
import type { ScheduleListItem } from '../types/schedule'
import { buildTree, flatten, sortDateOf } from './scheduleTree'

function item(
  id: number,
  overrides: Partial<ScheduleListItem> = {},
): ScheduleListItem {
  return {
    id,
    displayOrder: 0,
    title: `할 일 ${id}`,
    createdAt: '2026-09-08T00:00:00',
    updatedAt: '2026-09-08T00:00:00',
    ...overrides,
  }
}

describe('트리 묶기', () => {
  it('부모 밑으로 자식을 넣는다', () => {
    const tree = buildTree([item(1), item(2, { parentId: 1 })])

    expect(tree).toHaveLength(1)
    expect(tree[0].children.map((c) => c.item.id)).toEqual([2])
  })

  it('3단까지 이어 붙인다', () => {
    const rows = flatten(
      buildTree([item(1), item(2, { parentId: 1 }), item(3, { parentId: 2 })]),
    )

    expect(rows.map((r) => [r.item.id, r.depth])).toEqual([
      [1, 0],
      [2, 1],
      [3, 2],
    ])
  })

  it('부모를 못 찾으면 최상위로 올린다', () => {
    // 화면에서 항목이 사라지는 것이 가장 나쁜 실패다
    const tree = buildTree([item(1), item(2, { parentId: 999 })])

    expect(tree.map((n) => n.item.id)).toEqual([1, 2])
  })

  it('자기 자신을 부모로 가리켜도 돌지 않는다', () => {
    const tree = buildTree([item(1, { parentId: 1 })])

    expect(tree.map((n) => n.item.id)).toEqual([1])
  })

  it('기준을 안 주면 준 순서 그대로 둔다', () => {
    const tree = buildTree([
      item(1, { dueOn: '2026-12-01' }),
      item(2, { dueOn: '2026-01-01' }),
    ])

    expect(tree.map((n) => n.item.id)).toEqual([1, 2])
  })
})

describe('기한순 정렬', () => {
  it('기한이 없으면 시작일시의 날짜를 쓴다', () => {
    expect(sortDateOf(item(1, { startAt: '2026-10-01T09:00:00' }))).toBe(
      '2026-10-01',
    )
    expect(
      sortDateOf(item(1, { dueOn: '2026-09-01', startAt: '2026-10-01T09:00:00' })),
    ).toBe('2026-09-01')
    expect(sortDateOf(item(1))).toBeUndefined()
  })

  it('날짜가 없는 항목을 뒤로 보낸다', () => {
    const tree = buildTree(
      [item(1), item(2, { dueOn: '2026-10-05' }), item(3, { dueOn: '2026-10-01' })],
      'due',
    )

    expect(tree.map((n) => n.item.id)).toEqual([3, 2, 1])
  })

  it('형제 안에서만 바꾼다. 계층을 넘어 섞지 않는다', () => {
    const rows = flatten(
      buildTree(
        [
          item(1, { dueOn: '2026-12-01' }),
          item(2, { parentId: 1, dueOn: '2026-01-01' }),
          item(3, { dueOn: '2026-11-01' }),
        ],
        'due',
      ),
    )

    // 2 는 기한이 가장 이르지만 1 의 자식이라 1 밑에 그대로 있다
    expect(rows.map((r) => r.item.id)).toEqual([3, 1, 2])
  })
})
