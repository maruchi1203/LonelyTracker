import { describe, expect, it } from 'vitest'
import type { ScheduleListItem } from '../types/schedule'
import {
  buildTree,
  flatten,
  isDueSoon,
  selfAndDescendantIds,
  sortDateOf,
} from './scheduleTree'

function item(
  id: number,
  overrides: Partial<ScheduleListItem> = {},
): ScheduleListItem {
  return {
    id,
    displayOrder: 0,
    recurring: false,
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

describe('자기와 자손 모으기', () => {
  const tree = [
    item(1),
    item(2, { parentId: 1 }),
    item(3, { parentId: 2 }),
    item(4),
  ]

  it('자식이 없으면 자기 자신만 나온다', () => {
    expect([...selfAndDescendantIds(tree, 4)]).toEqual([4])
  })

  it('자식과 손자까지 모은다', () => {
    // 자기 자손을 상위로 삼으면 순환이 된다. 후보에서 빼야 한다
    expect([...selfAndDescendantIds(tree, 1)].sort()).toEqual([1, 2, 3])
  })

  it('형제는 넣지 않는다', () => {
    expect([...selfAndDescendantIds(tree, 2)].sort()).toEqual([2, 3])
  })
})

describe('우선순위 정렬', () => {
  it('높은 우선순위부터, 같으면 마감이 빠른 것부터', () => {
    const tree = buildTree(
      [
        item(1, { priority: 'COULD' }),
        item(2, { priority: 'MUST', dueOn: '2026-10-09' }),
        item(3, { priority: 'MUST', dueOn: '2026-10-01' }),
        item(4, { priority: 'WONT' }),
      ],
      'priority',
    )

    expect(tree.map((n) => n.item.id)).toEqual([3, 2, 1, 4])
  })

  it('안 정한 항목은 선택 자리에 둔다', () => {
    // 저장은 null 로 구분하고 정렬에서만 COULD 로 본다
    const tree = buildTree(
      [item(1, { priority: 'WONT' }), item(2), item(3, { priority: 'MUST' })],
      'priority',
    )

    expect(tree.map((n) => n.item.id)).toEqual([3, 2, 1])
  })

  it('형제 안에서만 세운다', () => {
    const rows = flatten(
      buildTree(
        [
          item(1, { priority: 'WONT' }),
          item(2, { parentId: 1, priority: 'MUST' }),
          item(3, { priority: 'MUST' }),
        ],
        'priority',
      ),
    )

    // 2 는 가장 높지만 1 의 자식이라 1 밑에 그대로 있다
    expect(rows.map((r) => r.item.id)).toEqual([3, 1, 2])
  })
})

describe('하루 안에 끝내야 하는 일', () => {
  const now = new Date('2026-09-30T14:00:00')

  it('기한이 오늘이면 급하다', () => {
    expect(isDueSoon(item(1, { dueOn: '2026-09-30' }), now)).toBe(true)
  })

  it('기한이 지났어도 급하다', () => {
    expect(isDueSoon(item(1, { dueOn: '2026-09-01' }), now)).toBe(true)
  })

  it('기한이 모레면 아직 아니다', () => {
    expect(isDueSoon(item(1, { dueOn: '2026-10-02' }), now)).toBe(false)
  })

  it('내일 기한은 그 날이 저물 때까지라 하루를 넘긴다', () => {
    // 2026-10-01T23:59:59 는 지금으로부터 34시간 뒤다
    expect(isDueSoon(item(1, { dueOn: '2026-10-01' }), now)).toBe(false)
  })

  it('기한이 없으면 시작일시로 잰다', () => {
    expect(isDueSoon(item(1, { startAt: '2026-09-30T18:00:00' }), now)).toBe(true)
    expect(isDueSoon(item(1, { startAt: '2026-10-05T18:00:00' }), now)).toBe(false)
  })

  it('반복은 시작일시가 아니라 이번 회차로 잰다', () => {
    const 반복 = { recurring: true, startAt: '2026-01-01T07:00:00' }

    // 규칙이 선 날로 재면 늘 지난 것이 되어 항상 급해진다
    expect(isDueSoon(item(1, { ...반복, occurrenceOn: '2026-09-30' }), now)).toBe(true)
    expect(isDueSoon(item(1, { ...반복, occurrenceOn: '2026-10-20' }), now)).toBe(false)
  })

  it('남은 회차가 없으면 급할 것도 없다', () => {
    expect(isDueSoon(item(1, { recurring: true }), now)).toBe(false)
  })

  it('끝낸 일은 급하지 않다', () => {
    const done = { dueOn: '2026-09-30', completedAt: '2026-09-30T10:00:00' }
    expect(isDueSoon(item(1, done), now)).toBe(false)
  })

  it('언제까지인지 정한 적이 없으면 조용하다', () => {
    expect(isDueSoon(item(1), now)).toBe(false)
  })
})
