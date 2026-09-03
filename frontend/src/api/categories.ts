import type { UserCategoryResponse } from '../types/schedule'
import { handle } from './http'

const BASE = '/api/categories'

export async function fetchCategories(): Promise<UserCategoryResponse[]> {
  return handle<UserCategoryResponse[]>(await fetch(BASE))
}

export async function createCategory(name: string, color?: string): Promise<UserCategoryResponse> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, color }),
  })
  return handle<UserCategoryResponse>(res)
}

export async function renameCategory(id: number, name: string): Promise<UserCategoryResponse> {
  const res = await fetch(`${BASE}/${id}/name`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return handle<UserCategoryResponse>(res)
}

/** 목록에서만 제거한다. 이 분류를 쓰던 일정은 그대로 남는다. */
export async function deleteCategory(id: number): Promise<void> {
  return handle<void>(await fetch(`${BASE}/${id}`, { method: 'DELETE' }))
}
