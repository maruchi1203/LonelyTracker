import type { ApiError, Schedule, ScheduleCreateRequest, ScheduleStatus } from '../types/schedule'

// vite.config.ts의 프록시가 /api 를 localhost:8080으로 넘긴다.
// 그래서 호스트를 하드코딩하지 않는다 — 배포 시 그대로 동작한다.
const BASE = '/api/schedules'

/** 응답을 공통 처리한다. 에러면 백엔드가 준 message를 그대로 던진다. */
async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `요청에 실패했습니다 (${res.status})`
    try {
      const body = (await res.json()) as ApiError
      if (body?.message) message = body.message
    } catch {
      // 본문이 JSON이 아니면 기본 메시지를 쓴다
    }
    throw new Error(message)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export async function fetchSchedules(params?: {
  from?: string
  to?: string
  status?: ScheduleStatus
}): Promise<Schedule[]> {
  const query = new URLSearchParams()
  if (params?.from) query.set('from', params.from)
  if (params?.to) query.set('to', params.to)
  if (params?.status) query.set('status', params.status)

  const suffix = query.toString() ? `?${query}` : ''
  return handle<Schedule[]>(await fetch(`${BASE}${suffix}`))
}

export async function createSchedule(body: ScheduleCreateRequest): Promise<Schedule> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return handle<Schedule>(res)
}

export async function changeStatus(id: number, status: ScheduleStatus): Promise<Schedule> {
  const res = await fetch(`${BASE}/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
  return handle<Schedule>(res)
}

export async function deleteSchedule(id: number): Promise<void> {
  return handle<void>(await fetch(`${BASE}/${id}`, { method: 'DELETE' }))
}
