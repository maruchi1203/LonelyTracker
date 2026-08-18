import type { Category } from '../types/schedule'
import { handle } from './http'

const BASE = '/api/categories'

export async function fetchCategories(): Promise<Category[]> {
  return handle<Category[]>(await fetch(BASE))
}

export async function renameCategory(id: number, name: string): Promise<Category> {
  const res = await fetch(`${BASE}/${id}/name`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return handle<Category>(res)
}

/** 하위 카테고리가 있으면 400. 이 분류를 쓰던 일정은 미분류로 남는다. */
export async function deleteCategory(id: number): Promise<void> {
  return handle<void>(await fetch(`${BASE}/${id}`, { method: 'DELETE' }))
}
