import type { OpenAiKeyStatus, UserResponse } from '../types/schedule'
import { handle } from './http'

const BASE = '/api/users/me'

export async function fetchMe(): Promise<UserResponse> {
  return handle<UserResponse>(await fetch(BASE))
}

/** 등록 여부와 마스킹된 꼬리만 온다 */
export async function fetchOpenAiKeyStatus(): Promise<OpenAiKeyStatus> {
  return handle<OpenAiKeyStatus>(await fetch(`${BASE}/openai-key`))
}

/** null 이나 빈 문자열을 보내면 등록을 해제한다 */
export async function changeOpenAiKey(apiKey: string | null): Promise<OpenAiKeyStatus> {
  const res = await fetch(`${BASE}/openai-key`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ apiKey }),
  })
  return handle<OpenAiKeyStatus>(res)
}
