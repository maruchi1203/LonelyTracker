import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'
import { isSameDay, toLocalDate } from '../utils/datetime'
import { fromMonthParam, toMonthParam } from '../utils/monthGrid'

/**
 * 달력의 화면 상태를 URL 에 둔다.
 * 새로고침·뒤로가기·링크 공유가 그대로 동작한다.
 */
export function useCalendarViewState() {
  const [params, setParams] = useSearchParams()

  const month = useMemo(() => fromMonthParam(params.get('ym')), [params])

  const selectedDate = useMemo(() => {
    const value = params.get('d')
    if (!value) return null
    const date = new Date(`${value}T00:00:00`)
    return Number.isNaN(date.getTime()) ? null : date
  }, [params])

  const update = useCallback(
    (mutate: (next: URLSearchParams) => void, replace = false) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          mutate(next)
          return next
        },
        { replace },
      )
    },
    [setParams],
  )

  const setMonth = useCallback(
    (next: Date) => {
      update((p) => {
        p.set('ym', toMonthParam(next))
        // 다른 달의 날짜가 선택된 채로 남으면 의미가 없다
        p.delete('d')
      })
    },
    [update],
  )

  /** 같은 날을 다시 고르면 선택을 푼다 */
  const toggleDate = useCallback(
    (date: Date) => {
      update((p) => {
        if (selectedDate && isSameDay(selectedDate, date)) p.delete('d')
        else p.set('d', toLocalDate(date))
      })
    },
    [update, selectedDate],
  )

  return { month, selectedDate, setMonth, toggleDate }
}
