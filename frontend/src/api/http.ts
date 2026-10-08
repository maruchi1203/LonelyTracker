import type { ApiError } from '../types/schedule'

/** 백엔드가 준 상태 코드를 함께 들고 다니는 에러 */
export class HttpError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'HttpError'
    this.status = status
  }
}

/** 응답 공통 처리. 에러면 백엔드가 준 message를 그대로 던진다. */
export async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `요청에 실패했습니다 (${res.status})`
    try {
      const body: ApiError = await res.json()
      if (body?.message) message = body.message
    } catch {
      // 본문이 JSON이 아니면 기본 메시지를 쓴다
    }
    throw new HttpError(res.status, message)
  }
  if (res.status === 204) {
    // SAFETY: 204 를 내는 길은 본문을 안 쓴다. 부르는 쪽이 T 를 void 로 둔다
    return undefined as T
  }
  // SAFETY: T 는 그 엔드포인트의 응답 DTO 다. 런타임에 확인할 방법이 없다
  return (await res.json()) as T
}
