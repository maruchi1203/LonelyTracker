import { describe, expect, it } from "vitest";
import { isInsecureUrl, presetOf, providerLabel, sameBaseUrl } from "./aiPresets";

describe("sameBaseUrl", () => {
  it("끝의 / 는 같은 주소로 본다", () => {
    expect(sameBaseUrl("https://api.openai.com/v1/", "https://api.openai.com/v1")).toBe(true);
  });

  it("경로가 다르면 다른 주소다", () => {
    expect(sameBaseUrl("https://api.openai.com/v1", "https://api.openai.com/v2")).toBe(false);
  });

  it("스킴과 호스트의 대소문자는 가리지 않는다", () => {
    expect(sameBaseUrl("HTTPS://API.OpenAI.com/v1", "https://api.openai.com/v1")).toBe(true);
  });

  it("기본 포트는 안 적은 것과 같다", () => {
    expect(sameBaseUrl("https://api.openai.com:443/v1", "https://api.openai.com/v1")).toBe(true);
  });

  it("경로의 대소문자는 가린다", () => {
    expect(sameBaseUrl("https://x.test/V1", "https://x.test/v1")).toBe(false);
  });
});

describe("isInsecureUrl", () => {
  it("http 는 대소문자와 상관없이 잡는다", () => {
    expect(isInsecureUrl("HTTP://localhost:11434/v1")).toBe(true);
  });

  it("https 는 잡지 않는다", () => {
    expect(isInsecureUrl("https://api.openai.com/v1")).toBe(false);
  });
});

describe("presetOf", () => {
  it("아는 주소면 그 프리셋을 준다", () => {
    expect(presetOf("https://api.anthropic.com/v1")?.id).toBe("claude");
  });

  it("직접 입력은 주소가 비어 있어도 고르지 않는다", () => {
    expect(presetOf("")).toBeUndefined();
  });
});

describe("providerLabel", () => {
  it("목록에 없으면 주소를 그대로 쓴다", () => {
    expect(providerLabel("http://localhost:11434/v1")).toBe("http://localhost:11434/v1");
  });
});
