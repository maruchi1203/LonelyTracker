import type { ScheduleResponse } from '../types/schedule'
import { toLocalDate } from '../utils/datetime'
import { instanceDateKeys, instanceKey } from './instance'

/** 한 칸에 보여줄 줄 수. 넘치면 "+N건" 으로 접는다 */
export const MAX_LANES = 4

const DAYS_PER_WEEK = 7

export interface LaneSlot {
  instance: ScheduleResponse
  /** 이 주에서 띠가 시작하는 날 */
  isStart: boolean
  /** 이 주에서 띠가 끝나는 날 */
  isEnd: boolean
}

export interface DayLanes {
  /** 레인 번호 그대로의 자리. 비어 있으면 null 이고, 화면에서는 자리만 차지한다 */
  lanes: (LaneSlot | null)[]
  /** 레인이 모자라 접힌 개수 */
  hidden: number
}

interface Segment {
  instance: ScheduleResponse
  /** 주 안에서의 칸 번호 (0~6) */
  from: number
  to: number
}

/**
 * 여러 날에 걸친 일정이 칸마다 같은 높이에 오도록 레인을 배정한다.
 *
 * 배정을 하지 않으면 같은 일정이 날마다 다른 줄에 놓여, 모서리를 이어 붙여도
 * 띠로 보이지 않는다.
 *
 * 주가 바뀌면 다시 배정한다 — 띠는 주 경계에서 끊어 그린다.
 */
export function assignLanes(
  days: Date[],
  instances: ScheduleResponse[],
): Map<string, DayLanes> {
  const result = new Map<string, DayLanes>()

  // 회차 하나가 걸치는 날짜를 미리 구해둔다
  const spans = instances.map((instance) => ({
    instance,
    keys: new Set(instanceDateKeys(instance)),
  }))

  for (let offset = 0; offset < days.length; offset += DAYS_PER_WEEK) {
    const week = days.slice(offset, offset + DAYS_PER_WEEK)
    const weekKeys = week.map(toLocalDate)

    const segments: Segment[] = []
    for (const { instance, keys } of spans) {
      const covered = weekKeys
        .map((key, i) => (keys.has(key) ? i : -1))
        .filter((i) => i >= 0)

      if (covered.length > 0) {
        segments.push({
          instance,
          from: covered[0],
          to: covered[covered.length - 1],
        })
      }
    }

    place(segments, weekKeys, result)
  }

  return result
}

function place(
  segments: Segment[],
  weekKeys: string[],
  result: Map<string, DayLanes>,
) {
  // 먼저 시작하는 것, 같으면 더 긴 것을 위쪽 레인에 둔다.
  // 마지막 기준은 순서를 매번 같게 만들기 위한 것이다 — 조회할 때마다 띠가 뛰면 안 된다
  const ordered = [...segments].sort(
    (a, b) =>
      a.from - b.from ||
      b.to - b.from - (a.to - a.from) ||
      instanceKey(a.instance).localeCompare(instanceKey(b.instance)),
  )

  const taken: boolean[][] = []
  for (const key of weekKeys) {
    result.set(key, { lanes: [], hidden: 0 })
  }

  for (const segment of ordered) {
    const lane = firstFreeLane(taken, segment)

    if (lane >= MAX_LANES) {
      for (let i = segment.from; i <= segment.to; i++) {
        result.get(weekKeys[i])!.hidden++
      }
      continue
    }

    for (let i = segment.from; i <= segment.to; i++) {
      taken[lane][i] = true

      const { lanes } = result.get(weekKeys[i])!
      while (lanes.length <= lane) lanes.push(null)
      lanes[lane] = {
        instance: segment.instance,
        isStart: i === segment.from,
        isEnd: i === segment.to,
      }
    }
  }
}

/** 겹치지 않는 가장 위쪽 레인. MAX_LANES 이상이면 접힌다 */
function firstFreeLane(taken: boolean[][], segment: Segment): number {
  for (let lane = 0; lane < MAX_LANES; lane++) {
    if (!taken[lane]) taken[lane] = []

    let free = true
    for (let i = segment.from; i <= segment.to; i++) {
      if (taken[lane][i]) {
        free = false
        break
      }
    }
    if (free) return lane
  }

  return MAX_LANES
}
