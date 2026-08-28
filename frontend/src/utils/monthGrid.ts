import { toLocalDateTime } from './datetime'

/** 한 달 그리드에 놓일 날짜들. 앞뒤 달의 날짜로 첫 주와 마지막 주를 채운다 */
export function buildMonthDays(month: Date): Date[] {
  const firstDate = new Date(month.getFullYear(), month.getMonth(), 1)
  const lastDate = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate()

  const prevMonthDates = firstDate.getDay() // 1일 앞에 채울 지난달 날짜 수
  const weeks = Math.ceil((prevMonthDates + lastDate) / 7)

  const start = new Date(firstDate)
  start.setDate(firstDate.getDate() - prevMonthDates)

  return Array.from({ length: weeks * 7 }, (_, i) => {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    return d
  })
}

/**
 * 조회할 기간. 그리드가 실제로 그리는 첫 칸부터 마지막 칸까지다.
 * 같은 함수가 칸과 기간을 함께 만들어야 둘이 어긋나지 않는다.
 */
export function monthGridWindow(month: Date): { from: string; to: string } {
  const days = buildMonthDays(month)
  const first = days[0]
  const last = days[days.length - 1]

  const from = new Date(first)
  from.setHours(0, 0, 0, 0)

  // 마지막 칸의 밤 일정까지 포함하려면 하루 끝으로 잡아야 한다
  const to = new Date(last)
  to.setHours(23, 59, 59, 0)

  return { from: toLocalDateTime(from), to: toLocalDateTime(to) }
}

/** "YYYY-MM" — 달을 URL 에 담는 형식 */
export function toMonthParam(month: Date): string {
  return `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}`
}

/** "YYYY-MM" 을 그 달 1일로. 형식이 어긋나면 이번 달을 준다 */
export function fromMonthParam(value: string | null): Date {
  const now = new Date()
  const thisMonth = new Date(now.getFullYear(), now.getMonth(), 1)

  const match = value?.match(/^(\d{4})-(\d{2})$/)
  if (!match) return thisMonth

  const month = Number(match[2])
  if (month < 1 || month > 12) return thisMonth

  return new Date(Number(match[1]), month - 1, 1)
}
