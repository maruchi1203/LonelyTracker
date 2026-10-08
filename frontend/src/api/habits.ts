import type {
  Habit,
  HabitCategory,
  HabitCategoryRequest,
  HabitParseResponse,
  HabitCreateRequest,
  HabitUpdateRequest,
} from '../types/habit'
import { handle } from './http'

const BASE = '/api/habits'

/** 카테고리는 길이 따로다. 습관이 하나도 없는 카테고리도 화면에 있어야 한다 */
const CATEGORIES = '/api/habit-categories'

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

/** 카테고리 전부. 화면에 늘어놓을 차례로 온다 */
export async function fetchHabitCategories(): Promise<HabitCategory[]> {
  const res = await fetch(CATEGORIES)

  return handle<HabitCategory[]>(res)
}

/** 맨 뒤에 붙는다 */
export async function createHabitCategory(
  body: HabitCategoryRequest,
): Promise<HabitCategory> {
  const res = await fetch(CATEGORIES, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  })

  return handle<HabitCategory>(res)
}

/** 이름만 고친다. 안의 습관은 id 로 붙어 있어 그대로다 */
export async function renameHabitCategory(
  id: number,
  body: HabitCategoryRequest,
): Promise<HabitCategory> {
  const res = await fetch(`${CATEGORIES}/${id}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  })

  return handle<HabitCategory>(res)
}

/** 카테고리와 그 안의 습관, 그 기록까지 함께 사라진다. 마지막 하나는 서버가 막는다(409) */
export async function deleteHabitCategory(id: number): Promise<void> {
  const res = await fetch(`${CATEGORIES}/${id}`, { method: 'DELETE' })

  return handle<void>(res)
}

/** 한 카테고리 안의 차례를 다시 세운다. 그 카테고리의 습관 전부를 보내야 한다 */
export async function reorderHabits(
  categoryId: number,
  ids: number[],
): Promise<void> {
  const res = await fetch(`${BASE}/order`, {
    method: 'PATCH',
    headers: JSON_HEADERS,
    body: JSON.stringify({ categoryId, ids }),
  })

  return handle<void>(res)
}

/** 카테고리 차례를 다시 세운다. 가진 카테고리 전부를 보내야 한다 */
export async function reorderHabitCategories(ids: number[]): Promise<void> {
  const res = await fetch(`${CATEGORIES}/order`, {
    method: 'PATCH',
    headers: JSON_HEADERS,
    body: JSON.stringify({ ids }),
  })

  return handle<void>(res)
}

/** 자연어 한 줄을 습관 초안으로 바꾼다. 저장은 하지 않는다 */
export async function parseHabit(
  text: string,
  signal?: AbortSignal,
): Promise<HabitParseResponse> {
  const res = await fetch(`${BASE}/parse`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ text }),
    signal,
  })

  return handle<HabitParseResponse>(res)
}
