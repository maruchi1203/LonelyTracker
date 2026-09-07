import type {
  OpenAiKeyStatus,
  UserResponse,
  UserSettings,
} from "../types/schedule";
import { handle } from "./http";

const BASE = "/api/users/me";

export async function fetchMe(): Promise<UserResponse> {
  return handle<UserResponse>(await fetch(BASE));
}

export async function fetchSettings(): Promise<UserSettings> {
  return handle<UserSettings>(await fetch(`${BASE}/settings`));
}

/** 바뀐 설정을 그대로 돌려준다 */
export async function changeSettings(
  settings: UserSettings,
): Promise<UserSettings> {
  const res = await fetch(`${BASE}/settings`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(settings),
  });
  return handle<UserSettings>(res);
}

/** 등록 여부와 마스킹된 꼬리만 온다 */
export async function fetchOpenAiKeyStatus(): Promise<OpenAiKeyStatus> {
  return handle<OpenAiKeyStatus>(await fetch(`${BASE}/openai-key`));
}

/** API Key 등록 혹은 수정
 * null이나 빈 문자열을 보내면 등록을 해제한다 */
export async function changeOpenAiKey(
  apiKey: string | null,
): Promise<OpenAiKeyStatus> {
  const res = await fetch(`${BASE}/openai-key`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ apiKey }),
  });
  return handle<OpenAiKeyStatus>(res);
}
