import type { ParsedSchedule } from '../types/parse'
import type {
  DeleteScope,
  InstanceUpdateRequest,
  ScheduleCreateRequest,
  ScheduleQuery,
  ScheduleResponse,
  ScheduleStatus,
} from '../types/schedule'
import { handle } from './http'

// vite.config.ts의 프록시가 /api 를 localhost:8080으로 넘긴다.
// 그래서 호스트를 하드코딩하지 않는다 — 배포 시 그대로 동작한다.
const BASE = '/api/schedules'

const JSON_HEADERS = { 'Content-Type': 'application/json' }

export async function fetchSchedules(
  params?: ScheduleQuery,
  signal?: AbortSignal,
): Promise<ScheduleResponse[]> {
  const query = new URLSearchParams()
  if (params?.from) query.set('from', params.from)
  if (params?.to) query.set('to', params.to)
  if (params?.status) query.set('status', params.status)
  if (params?.tag) query.set('tag', params.tag)

  const suffix = query.toString() ? `?${query}` : ''
  return handle<ScheduleResponse[]>(await fetch(`${BASE}${suffix}`, { signal }))
}

export async function createSchedule(
  body: ScheduleCreateRequest,
): Promise<ScheduleResponse> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  })
  return handle<ScheduleResponse>(res)
}

/** 회차 하나의 상태를 바꾼다. 습관 전용이다 */
export async function changeInstanceStatus(
  id: number,
  onDate: string,
  status: ScheduleStatus,
): Promise<ScheduleResponse> {
  const res = await fetch(`${BASE}/${id}/instances/${onDate}/status`, {
    method: 'PATCH',
    headers: JSON_HEADERS,
    body: JSON.stringify({ status }),
  })
  return handle<ScheduleResponse>(res)
}

/** 회차 하나만 고친다. 생략한 칸은 일정의 값으로 되돌아간다 */
export async function updateInstance(
  id: number,
  onDate: string,
  body: InstanceUpdateRequest,
): Promise<ScheduleResponse> {
  const res = await fetch(`${BASE}/${id}/instances/${onDate}`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify(body),
  })
  return handle<ScheduleResponse>(res)
}

/** 1회성 일정을 완료하거나 되돌린다. 습관은 회차 상태를 쓴다 */
export async function changeCompletion(
  id: number,
  completed: boolean,
): Promise<ScheduleResponse> {
  const res = await fetch(`${BASE}/${id}/completion`, {
    method: 'PATCH',
    headers: JSON_HEADERS,
    body: JSON.stringify({ completed }),
  })
  return handle<ScheduleResponse>(res)
}

/** 이미 쓴 적 있는 태그 이름. 입력 자동완성이 쓴다 */
export async function fetchTagNames(): Promise<string[]> {
  return handle<string[]>(await fetch(`${BASE}/tags`))
}

/** 문장을 일정 초안으로 바꾼다. 저장하지는 않는다 */
export async function parseSchedule(
  text: string,
  signal?: AbortSignal,
): Promise<ParsedSchedule> {
  const res = await fetch(`${BASE}/parse`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ text }),
    signal,
  })
  return handle<ParsedSchedule>(res)
}

/** FUTURE 는 지난 기록을 남기고 앞으로만 지운다. 호출하는 쪽이 범위를 밝히게 한다 */
export async function deleteSchedule(id: number, scope: DeleteScope): Promise<void> {
  return handle<void>(await fetch(`${BASE}/${id}?scope=${scope}`, { method: 'DELETE' }))
}
