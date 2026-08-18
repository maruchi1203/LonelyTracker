import type { Schedule, ScheduleCreateRequest, ScheduleQuery, ScheduleStatus } from '../types/schedule'
import { handle } from './http'

// vite.config.ts의 프록시가 /api 를 localhost:8080으로 넘긴다.
// 그래서 호스트를 하드코딩하지 않는다 — 배포 시 그대로 동작한다.
const BASE = '/api/schedules'

export async function fetchSchedules(params?: ScheduleQuery): Promise<Schedule[]> {
  const query = new URLSearchParams()
  if (params?.from) query.set('from', params.from)
  if (params?.to) query.set('to', params.to)
  if (params?.status) query.set('status', params.status)
  // URLSearchParams가 역슬래시를 알아서 인코딩한다
  if (params?.category) query.set('category', params.category)

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
