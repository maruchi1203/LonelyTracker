/** 설정 화면에서 고르는 AI 제공자. 백엔드는 이 목록을 모르고 주소·모델·키만 받는다 */
export interface AiPreset {
  id: string;
  label: string;
  baseUrl: string;
  /** 저장할 때 채워 넣는 모델. 사용자가 바꿀 수 있다 */
  model: string;
  /** 응답 형식을 제공자가 보장하는지 */
  strict: boolean;
}

export const CUSTOM_PRESET_ID = "custom";

export const AI_PRESETS: AiPreset[] = [
  {
    id: "openai",
    label: "OpenAI",
    baseUrl: "https://api.openai.com/v1",
    model: "gpt-5.6-luna",
    strict: true,
  },
  {
    id: "gemini",
    label: "Gemini",
    baseUrl: "https://generativelanguage.googleapis.com/v1beta/openai",
    model: "gemini-2.5-flash",
    strict: true,
  },
  {
    id: "claude",
    label: "Claude",
    baseUrl: "https://api.anthropic.com/v1",
    model: "claude-haiku-4-5-20251001",
    strict: true,
  },
  {
    id: CUSTOM_PRESET_ID,
    label: "직접 입력",
    baseUrl: "",
    model: "",
    strict: false,
  },
];

/**
 * 같은 서버를 가리키는 주소를 한 모양으로 맞춘다
 * 스킴·호스트는 소문자, 기본 포트와 끝의 / 는 뗀다. 경로는 건드리지 않는다
 */
export function canonicalBaseUrl(url: string): string {
  const trimmed = url.trim();
  try {
    const parsed = new URL(trimmed);
    return (parsed.origin + parsed.pathname).replace(/\/+$/, "");
  } catch {
    return trimmed.replace(/\/+$/, "");
  }
}

export function sameBaseUrl(a: string, b: string): boolean {
  return canonicalBaseUrl(a) === canonicalBaseUrl(b);
}

/** 암호화되지 않은 주소인지. 키와 문장이 그대로 오간다 */
export function isInsecureUrl(url: string): boolean {
  return canonicalBaseUrl(url).startsWith("http://");
}

/** 주소에 맞는 프리셋. 목록에 없으면 undefined */
export function presetOf(baseUrl: string): AiPreset | undefined {
  return AI_PRESETS.find(
    (p) => p.id !== CUSTOM_PRESET_ID && sameBaseUrl(p.baseUrl, baseUrl),
  );
}

/** 화면에 적을 제공자 이름. 목록에 없으면 주소를 그대로 쓴다 */
export function providerLabel(baseUrl: string): string {
  return presetOf(baseUrl)?.label ?? baseUrl;
}
