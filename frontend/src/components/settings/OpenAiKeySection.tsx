import { useCallback, useEffect, useRef, useState } from "react";
import { changeOpenAiKey, fetchOpenAiKeyStatus } from "../../api/users";
import { HttpError } from "../../api/http";
import type { OpenAiKeyStatus } from "../../types/schedule";

interface Props {
  /** 다른 화면에서 키를 등록하라고 보냈을 때 입력칸으로 바로 데려간다 */
  autoFocus?: boolean;
}

const INPUT =
  "w-full rounded-md border border-slate-200 bg-white px-2.5 py-2 text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100";

/**
 * 503 은 세 가지를 뜻한다 — 키 없음, 서버 암호화 키 미설정, 복호화 실패.
 * 뒤의 둘은 사용자가 키를 다시 넣어도 해결되지 않으므로 구분해서 알린다.
 */
function describe(e: unknown, fallback: string): string {
  if (e instanceof HttpError && e.status === 503) {
    return `${e.message} — 입력한 키의 문제가 아니라 서버 설정 문제입니다.`;
  }
  return e instanceof Error ? e.message : fallback;
}

export default function OpenAiKeySection({ autoFocus }: Props) {
  const [status, setStatus] = useState<OpenAiKeyStatus | null>(null);
  const [apiKey, setApiKey] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    try {
      setStatus(await fetchOpenAiKeyStatus());
    } catch (e) {
      setError(describe(e, "키 상태를 불러오지 못했습니다"));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (autoFocus) inputRef.current?.focus();
  }, [autoFocus]);

  const submit = async (value: string | null, done: string) => {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      setStatus(await changeOpenAiKey(value));
      // 저장한 키를 화면에 남겨두지 않는다
      setApiKey("");
      setNotice(done);
    } catch (e) {
      setError(describe(e, "키를 저장하지 못했습니다"));
    } finally {
      setBusy(false);
    }
  };

  const registered = status?.registered === true;

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-slate-800">OpenAI API 키</h3>

        <span
          className={`rounded-full border px-2.5 py-0.5 text-xs font-medium ${
            registered
              ? "border-brand-100 bg-brand-50 text-brand-700"
              : "border-slate-200 bg-slate-50 text-slate-500"
          }`}
        >
          {registered ? `등록됨 · ${status?.masked ?? ""}` : "미등록"}
        </span>
      </div>

      <p className="text-sm text-slate-500">
        {registered
          ? "자연어로 일정을 입력할 수 있습니다."
          : "자연어 입력을 쓰려면 키가 필요합니다. 키는 암호화해 저장하며 다시 보여주지 않습니다."}
      </p>

      <form
        className="flex flex-wrap items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          if (apiKey.trim()) void submit(apiKey.trim(), "키를 저장했습니다.");
        }}
      >
        <input
          ref={inputRef}
          type="password"
          className={`${INPUT} min-w-0 flex-1 basis-64`}
          value={apiKey}
          onChange={(e) => setApiKey(e.target.value)}
          placeholder={registered ? "새 키로 바꾸려면 입력하세요" : "sk-..."}
          maxLength={300}
          autoComplete="off"
          aria-label="OpenAI API 키"
        />

        <button
          type="submit"
          className="rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={busy || !apiKey.trim()}
        >
          저장
        </button>

        {registered && (
          <button
            type="button"
            className="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-600 transition-colors hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
            disabled={busy}
            onClick={() => void submit(null, "키를 지웠습니다.")}
          >
            해제
          </button>
        )}
      </form>

      {notice && <p className="text-sm text-brand-700">{notice}</p>}
      {error && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-600">
          {error}
        </p>
      )}
    </section>
  );
}
