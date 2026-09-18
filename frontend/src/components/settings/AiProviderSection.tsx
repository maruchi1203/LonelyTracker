import { useCallback, useEffect, useRef, useState } from "react";
import {
  activateAiProvider,
  deleteAiProvider,
  fetchAiProviders,
  saveAiProvider,
} from "../../api/users";
import { HttpError } from "../../api/http";
import {
  AI_PRESETS,
  CUSTOM_PRESET_ID,
  isInsecureUrl,
  providerLabel,
  sameBaseUrl,
} from "../../constants/aiPresets";
import type { AiProviderList } from "../../types/schedule";

interface Props {
  /** 다른 화면에서 키를 등록하라고 보냈을 때 입력칸으로 바로 데려간다 */
  autoFocus?: boolean;
}

const INPUT =
  "w-full rounded-md border border-slate-200 bg-white px-2.5 py-2 text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100";

/**
 * 503 은 서버 암호화 키 미설정이나 복호화 실패를 뜻한다.
 * 사용자가 키를 다시 넣어도 해결되지 않으므로 구분해서 알린다.
 */
function describe(e: unknown, fallback: string): string {
  if (e instanceof HttpError && e.status === 503) {
    return `${e.message} — 입력한 키의 문제가 아니라 서버 설정 문제입니다.`;
  }
  return e instanceof Error ? e.message : fallback;
}

export default function AiProviderSection({ autoFocus }: Props) {
  const [list, setList] = useState<AiProviderList | null>(null);
  const [presetId, setPresetId] = useState(AI_PRESETS[0].id);
  const [baseUrl, setBaseUrl] = useState(AI_PRESETS[0].baseUrl);
  const [model, setModel] = useState(AI_PRESETS[0].model);
  const [apiKey, setApiKey] = useState("");
  const [limit, setLimit] = useState("");
  const [allowHttp, setAllowHttp] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const keyRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    try {
      setList(await fetchAiProviders());
    } catch (e) {
      setError(describe(e, "제공자 설정을 불러오지 못했습니다"));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (autoFocus) keyRef.current?.focus();
  }, [autoFocus]);

  const preset = AI_PRESETS.find((p) => p.id === presetId) ?? AI_PRESETS[0];
  const custom = preset.id === CUSTOM_PRESET_ID;
  const providers = list?.providers ?? [];
  const active = providers.find((c) => c.active);
  // 같은 주소가 이미 있으면 키를 비워도 모델만 바뀐다
  const existing = providers.find((c) => sameBaseUrl(c.baseUrl, baseUrl));
  const insecure = isInsecureUrl(baseUrl);
  const canSave =
    baseUrl.trim() !== "" &&
    model.trim() !== "" &&
    (existing !== undefined || apiKey.trim() !== "") &&
    // http 는 막지 않되 사용자가 알고 고르게 한다
    (!insecure || allowHttp);

  // 이미 등록한 제공자를 고르면 그 한도를 보여 준다. 비운 채로 저장해 지워지지 않게
  const savedLimit = existing?.monthlyTokenLimit;
  useEffect(() => {
    setLimit(savedLimit === undefined ? "" : String(savedLimit));
  }, [savedLimit]);

  const choosePreset = (id: string) => {
    const next = AI_PRESETS.find((p) => p.id === id) ?? AI_PRESETS[0];
    setPresetId(next.id);
    setBaseUrl(next.baseUrl);
    setModel(next.model);
    setAllowHttp(false);
  };

  const run = async (action: () => Promise<unknown>, done: string) => {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(done);
      await load();
    } catch (e) {
      setError(describe(e, "요청을 처리하지 못했습니다"));
    } finally {
      setBusy(false);
    }
  };

  const badge = active
    ? `사용 중 · ${providerLabel(active.baseUrl)}`
    : list?.serverConfigured
      ? ".env 설정 사용 중"
      : "미등록";

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-slate-800">AI 제공자</h3>

        <span
          className={`rounded-full border px-2.5 py-0.5 text-xs font-medium ${
            active || list?.serverConfigured
              ? "border-brand-100 bg-brand-50 text-brand-700"
              : "border-slate-200 bg-slate-50 text-slate-500"
          }`}
        >
          {badge}
        </span>
      </div>

      <p className="text-sm text-slate-500">
        자연어 입력에 쓸 제공자와 키입니다. 제공자마다 키를 따로 저장하므로
        바꿔도 앞서 넣은 키가 남습니다. 키는 암호화해 저장하며 다시 보여주지
        않습니다.
      </p>

      {!active && list?.serverConfigured && (
        <p className="text-xs text-slate-400">
          지금은 backend/.env 의 설정으로 부릅니다. 여기서 고르면 이쪽이
          먼저입니다.
        </p>
      )}

      {providers.length > 0 && (
        <ul className="flex list-none flex-col gap-1 p-0">
          {providers.map((c) => (
            <li
              key={c.id}
              className="flex flex-wrap items-center gap-2 rounded-md border border-slate-100 px-3 py-2 text-sm"
            >
              <span className="font-medium text-slate-700">
                {providerLabel(c.baseUrl)}
              </span>
              <span className="text-xs text-slate-400">
                {c.model} · {c.masked}
              </span>
              <span className="flex-1" />

              {c.active ? (
                <span className="rounded-full bg-brand-50 px-2 py-0.5 text-xs text-brand-700">
                  사용 중
                </span>
              ) : (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() =>
                    void run(
                      () => activateAiProvider(c.id),
                      `${providerLabel(c.baseUrl)}(으)로 전환했습니다.`,
                    )
                  }
                  className="rounded-md border border-slate-200 px-2.5 py-1 text-xs text-slate-600 transition-colors hover:bg-brand-50 disabled:opacity-50"
                >
                  사용
                </button>
              )}

              <button
                type="button"
                disabled={busy}
                onClick={() => {
                  if (!window.confirm(`${providerLabel(c.baseUrl)} 키를 지울까요?`)) {
                    return;
                  }
                  void run(() => deleteAiProvider(c.id), "키를 지웠습니다.");
                }}
                className="rounded-md border border-transparent px-2 py-1 text-xs text-red-500 transition-colors hover:border-red-200 hover:bg-red-50 disabled:opacity-50"
              >
                삭제
              </button>
            </li>
          ))}
        </ul>
      )}

      <form
        className="flex flex-col gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          if (!canSave) return;
          const label = providerLabel(baseUrl.trim());
          void run(async () => {
            await saveAiProvider({
              baseUrl: baseUrl.trim(),
              model: model.trim(),
              apiKey: apiKey.trim() || undefined,
              monthlyTokenLimit: limit.trim() === "" ? undefined : Number(limit),
            });
            // 저장한 키를 화면에 남겨두지 않는다
            setApiKey("");
          }, `${label} 키를 저장했습니다. 이제 이 제공자로 부릅니다.`);
        }}
      >
        <div className="flex flex-wrap gap-2">
          <label className="flex min-w-0 flex-1 basis-40 flex-col gap-1">
            <span className="text-xs font-semibold text-slate-500">제공자</span>
            <select
              value={presetId}
              onChange={(e) => choosePreset(e.target.value)}
              className={INPUT}
            >
              {AI_PRESETS.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.label}
                </option>
              ))}
            </select>
          </label>

          <label className="flex min-w-0 flex-1 basis-40 flex-col gap-1">
            <span className="text-xs font-semibold text-slate-500">모델 *</span>
            <input
              value={model}
              onChange={(e) => setModel(e.target.value)}
              placeholder="예: gemini-2.5-flash"
              maxLength={100}
              className={INPUT}
            />
          </label>
        </div>

        <label className="flex flex-col gap-1">
          <span className="text-xs font-semibold text-slate-500">
            이번 달 토큰 한도
          </span>
          <input
            type="number"
            min={1}
            value={limit}
            onChange={(e) => setLimit(e.target.value)}
            placeholder="비우면 한도 없음. 넘으면 AI 를 부르지 않습니다"
            className={INPUT}
          />
        </label>

        {custom && (
          <label className="flex flex-col gap-1">
            <span className="text-xs font-semibold text-slate-500">주소 *</span>
            <input
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              placeholder="OpenAI 호환 주소. 예: http://localhost:11434/v1"
              maxLength={300}
              className={INPUT}
            />
          </label>
        )}

        {insecure && (
          <label className="flex cursor-pointer items-start gap-2 rounded-md border border-rose-300 bg-rose-50 px-3 py-2 text-xs text-rose-700 select-none">
            <input
              type="checkbox"
              checked={allowHttp}
              onChange={(e) => setAllowHttp(e.target.checked)}
              className="mt-0.5 size-4 shrink-0 cursor-pointer accent-rose-600"
            />
            <span>
              암호화되지 않은 http 주소입니다. API 키와 입력한 문장이 그대로
              오갑니다. 자기 컴퓨터의 로컬 제공자처럼 믿을 수 있는 곳일 때만
              켜세요.
            </span>
          </label>
        )}

        {!preset.strict && (
          <p className="text-xs text-amber-600">
            목록에 없는 제공자는 응답 형식을 보장하지 못할 수 있습니다. 형식을
            어긴 응답이 오면 파싱이 실패합니다.
          </p>
        )}

        <div className="flex flex-wrap items-center gap-2">
          <input
            ref={keyRef}
            type="password"
            className={`${INPUT} min-w-0 flex-1 basis-64`}
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder={
              existing ? "비우면 기존 키를 그대로 둡니다" : "API 키"
            }
            maxLength={300}
            autoComplete="off"
            aria-label="API 키"
          />

          <button
            type="submit"
            className="rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
            disabled={busy || !canSave}
          >
            저장
          </button>
        </div>
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
