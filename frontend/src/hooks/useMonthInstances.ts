import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchSchedules } from '../api/schedules'
import { replaceInstance } from '../domain/instance'
import type { ScheduleResponse } from '../types/schedule'
import { monthGridWindow } from '../utils/monthGrid'

interface MonthInstances {
  instances: ScheduleResponse[]
  loading: boolean
  error: string | null
  reload: () => Promise<void>
  /** 회차 하나만 갈아끼운다. 같은 id 의 다른 회차는 그대로 둔다 */
  patchOne: (updated: ScheduleResponse) => void
  setError: (message: string | null) => void
}

/**
 * 달력에 보이는 기간의 회차를 가져온다.
 * 분류·검색은 서버에 넘기지 않는다 — 창 전체를 받아야 화면에서 빈도를 셀 수 있다.
 */
export function useMonthInstances(month: Date): MonthInstances {
  const [instances, setInstances] = useState<ScheduleResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const { from, to } = monthGridWindow(month)

  // 늦게 도착한 응답이 최신 결과를 덮지 않게 한다.
  // StrictMode 가 effect 를 두 번 돌리므로 abort 만으로는 부족하다.
  const requestSeq = useRef(0)

  const run = useCallback(async (signal?: AbortSignal) => {
    const seq = ++requestSeq.current
    setLoading(true)
    setError(null)
    try {
      const list = await fetchSchedules({ from, to }, signal)
      if (seq === requestSeq.current) setInstances(list)
    } catch (e) {
      if (signal?.aborted) return
      if (seq === requestSeq.current) {
        setError(e instanceof Error ? e.message : '목록을 불러오지 못했습니다')
      }
    } finally {
      if (seq === requestSeq.current) setLoading(false)
    }
  }, [from, to])

  useEffect(() => {
    const controller = new AbortController()
    void run(controller.signal)
    return () => controller.abort()
  }, [run])

  const reload = useCallback(() => run(), [run])

  const patchOne = useCallback((updated: ScheduleResponse) => {
    setInstances((prev) => replaceInstance(prev, updated))
  }, [])

  return { instances, loading, error, reload, patchOne, setError }
}
