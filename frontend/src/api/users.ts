import type {
  AiProvider,
  AiProviderList,
  AiProviderRequest,
  AiUsageSummary,
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

const CREDENTIALS = `${BASE}/ai-providers`;

/** 등록한 제공자 설정들. 키는 마스킹된 꼬리만 온다 */
export async function fetchAiProviders(): Promise<AiProviderList> {
  return handle<AiProviderList>(await fetch(CREDENTIALS));
}

/** 주소가 같으면 고치고 없으면 만든다. 저장한 것을 바로 쓴다 */
export async function saveAiProvider(
  body: AiProviderRequest,
): Promise<AiProvider> {
  const res = await fetch(CREDENTIALS, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return handle<AiProvider>(res);
}

/** 이 제공자 설정으로 부르게 한다 */
export async function activateAiProvider(id: number): Promise<AiProvider> {
  const res = await fetch(`${CREDENTIALS}/${id}/active`, { method: "PUT" });
  return handle<AiProvider>(res);
}

/** 이번 주와 이번 달의 제공자별 호출 수·토큰 */
export async function fetchAiUsage(): Promise<AiUsageSummary> {
  return handle<AiUsageSummary>(await fetch(`${BASE}/ai-usage`));
}

export async function deleteAiProvider(id: number): Promise<void> {
  const res = await fetch(`${CREDENTIALS}/${id}`, { method: "DELETE" });
  return handle<void>(res);
}
