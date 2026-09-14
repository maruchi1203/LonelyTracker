import type {
  AiCredential,
  AiCredentialList,
  AiCredentialRequest,
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

const CREDENTIALS = `${BASE}/ai-credentials`;

/** 등록한 자격 증명들. 키는 마스킹된 꼬리만 온다 */
export async function fetchAiCredentials(): Promise<AiCredentialList> {
  return handle<AiCredentialList>(await fetch(CREDENTIALS));
}

/** 주소가 같으면 고치고 없으면 만든다. 저장한 것을 바로 쓴다 */
export async function saveAiCredential(
  body: AiCredentialRequest,
): Promise<AiCredential> {
  const res = await fetch(CREDENTIALS, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return handle<AiCredential>(res);
}

/** 이 자격 증명으로 부르게 한다 */
export async function activateAiCredential(id: number): Promise<AiCredential> {
  const res = await fetch(`${CREDENTIALS}/${id}/active`, { method: "PUT" });
  return handle<AiCredential>(res);
}

export async function deleteAiCredential(id: number): Promise<void> {
  const res = await fetch(`${CREDENTIALS}/${id}`, { method: "DELETE" });
  return handle<void>(res);
}
