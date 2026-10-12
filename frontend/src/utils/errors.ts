// oxlint-disable anti-slop/no-unknown-parameters

/*
 * throw를 다루는 함수 모음집.
 *
 * 아무나 `throw "string"` 을 할 수 있어 catch 는 unknown 을 준다.
 * 좁히는 일을 여기서만 한다.
 */

/**
 * throw를 Error로 선언한다.
 * 문자열이 아니라 Error를 돌려주면 원래 무슨 class였는지 확인 가능하다.
 *
 * @param fallback Error 가 아닌 것이 던져졌을 때 쓸 문구
 */
export function asError(thrown: unknown, fallback: string): Error {
  return thrown instanceof Error ? thrown : new Error(fallback);
}
