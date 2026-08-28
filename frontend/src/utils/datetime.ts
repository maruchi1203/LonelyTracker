// 백엔드는 타임존이 없는 LocalDateTime/LocalDate 를 쓴다.
// toISOString() 은 UTC 로 당기고 Z 를 붙이므로 여기서는 절대 쓰지 않는다.

const p = (n: number) => String(n).padStart(2, '0')

/** datetime-local 입력이 요구하는 "YYYY-MM-DDTHH:mm" */
export function toLocalInputValue(date: Date): string {
  return `${toLocalDate(date)}T${p(date.getHours())}:${p(date.getMinutes())}`
}

/** 백엔드 LocalDateTime 이 요구하는 "YYYY-MM-DDTHH:mm:ss" */
export function toLocalDateTime(date: Date): string {
  return `${toLocalInputValue(date)}:${p(date.getSeconds())}`
}

/** 백엔드 LocalDate 가 요구하는 "YYYY-MM-DD" */
export function toLocalDate(date: Date): string {
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
}

/** 두 시각이 같은 날인지 (시·분은 무시) */
export function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

/** "HH:mm" */
export function formatTime(date: Date): string {
  return `${p(date.getHours())}:${p(date.getMinutes())}`
}

/**
 * 기본 시작 시각. 지금 이후의 가장 가까운 정각으로 잡는다.
 * 날짜를 골라둔 상태면 그 날짜에 같은 시각을 얹는다.
 */
export function nextHour(baseDate?: Date | null): string {
  const now = new Date()
  const at = new Date(now)
  at.setHours(now.getHours() + 1, 0, 0, 0)

  if (baseDate) {
    at.setFullYear(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate())
  }
  return toLocalInputValue(at)
}
