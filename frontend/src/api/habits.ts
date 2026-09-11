import type {
  Habit,
  HabitCreateRequest,
  HabitUpdateRequest,
} from '../types/habit'
import { handle } from './http'

const BASE = '/api/habits'

const JSON_HEADERS = { 'Content-Type': 'application/json' }

/** 목록과 그 구간의 기록. 비우면 서버가 최근 2주를 준다 */
export async function fetchHabits(
  from?: string,
  to?: string,
): Promise<Habit[]> {
  const query = new URLSearchParams()
  if (from) query.set('from', from)
  if (to) query.set('to', to)

  const res = await fetch(query.size > 0 ? `${BASE}?${query}` : BASE)
  return handle<Habit[]>(res)
}

export async function createHabit(body: HabitCreateRequest): Promise<Habit> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  })
  return handle<Habit>(res)
}

export async function updateHabit(
  id: number,
  body: HabitUpdateRequest,
): Promise<Habit> {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  })
  return handle<Habit>(res)
}

/** 그날 해냈는지 표시한다. done 이 false 면 그날 기록을 지운다 */
export async function logHabit(
  id: number,
  onDate: string,
  done: boolean,
  note?: string,
): Promise<void> {
  const res = await fetch(`${BASE}/${id}/logs/${onDate}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify({ done, note }),
  })
  return handle<void>(res)
}

/** 그만두거나 다시 시작한다. 지난 기록은 남는다 */
export async function archiveHabit(
  id: number,
  archived: boolean,
): Promise<Habit> {
  const res = await fetch(`${BASE}/${id}/archived?archived=${archived}`, {
    method: 'PATCH',
  })
  return handle<Habit>(res)
}

export async function deleteHabit(id: number): Promise<void> {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' })
  return handle<void>(res)
}
