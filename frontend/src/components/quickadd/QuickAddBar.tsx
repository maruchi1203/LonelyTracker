import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { HttpError } from "../../api/http";
import { parseSchedule } from "../../api/schedules";
import { fetchOpenAiKeyStatus } from "../../api/users";
import { knownQuestions } from "../../constants/parseQuestions";
import type { Draft } from "../../domain/draft";
import { draftFromParsed, draftToCreateRequest } from "../../domain/draft";
import type { ParseQuestion } from "../../types/parse";
import type { ScheduleCreateRequest } from "../../types/schedule";
import ScheduleInputForm from "../ScheduleInputForm";
import ParsedDraftCard from "./ParsedDraftCard";

interface Props {
  defaultDate: Date | null;
  knownCategories: string[];
  onCreate: (body: ScheduleCreateRequest) => Promise<boolean>;
}

type State =
  | { mode: "idle" }
  | { mode: "parsing" }
  | { mode: "draft"; draft: Draft; questions: ParseQuestion[] }
  | { mode: "saving"; draft: Draft; questions: ParseQuestion[] }
  | { mode: "error"; message: string; needsKey: boolean };

/** 서버 읽기 타임아웃이 30초라 그보다 조금 뒤에 포기한다 */
const GIVE_UP_MS = 35_000;
const STEPS = ["문장을 읽는 중…", "일정으로 바꾸는 중…", "거의 다 됐어요…"];
const STEP_MS = 2_500;
/** 설정에 다녀오는 동안 친 문장을 잃지 않게 둘 자리 */
const DRAFT_TEXT_KEY = "quickadd-text";

export default function QuickAddBar({
  defaultDate,
  knownCategories,
  onCreate,
}: Props) {
  const [text, setText] = useState(
    () => sessionStorage.getItem(DRAFT_TEXT_KEY) ?? "",
  );
  const [state, setState] = useState<State>({ mode: "idle" });
  const [manual, setManual] = useState(false);
  const [step, setStep] = useState(0);
  const abort = useRef<AbortController | null>(null);

  const parsing = state.mode === "parsing";

  useEffect(() => {
    if (!parsing) return;
    setStep(0);
    const timer = window.setInterval(
      () => setStep((s) => Math.min(s + 1, STEPS.length - 1)),
      STEP_MS,
    );
    return () => window.clearInterval(timer);
  }, [parsing]);

  const stop = () => {
    abort.current?.abort();
    abort.current = null;
  };

  useEffect(() => stop, []);

  const parse = async () => {
    const sentence = text.trim();
    if (!sentence) return;

    const controller = new AbortController();
    abort.current = controller;
    const giveUp = window.setTimeout(() => controller.abort(), GIVE_UP_MS);
    setState({ mode: "parsing" });

    try {
      const parsed = await parseSchedule(sentence, controller.signal);
      setState({
        mode: "draft",
        draft: draftFromParsed(parsed, defaultDate),
        questions: knownQuestions(parsed.questions),
      });
      sessionStorage.removeItem(DRAFT_TEXT_KEY);
    } catch (e) {
      if (controller.signal.aborted) {
        setState({
          mode: "error",
          message: "응답이 너무 늦습니다. 직접 입력해 주세요.",
          needsKey: false,
        });
        return;
      }
      // 503 은 키 없음 말고도 서버 암호화 문제일 수 있어 상태를 한 번 더 확인한다
      let needsKey = false;
      if (e instanceof HttpError && e.status === 503) {
        needsKey = await fetchOpenAiKeyStatus()
          .then((s) => !s.registered)
          .catch(() => false);
        if (needsKey) sessionStorage.setItem(DRAFT_TEXT_KEY, sentence);
      }
      setState({
        mode: "error",
        message: e instanceof Error ? e.message : "문장을 읽지 못했습니다",
        needsKey,
      });
    } finally {
      window.clearTimeout(giveUp);
      abort.current = null;
    }
  };

  const save = async () => {
    if (state.mode !== "draft") return;
    const { draft, questions } = state;
    setState({ mode: "saving", draft, questions });

    const created = await onCreate(draftToCreateRequest(draft));
    if (created) {
      setText("");
      setState({ mode: "idle" });
    } else {
      setState({ mode: "draft", draft, questions });
    }
  };

  const patch = (changes: Partial<Draft>) =>
    setState((prev) =>
      prev.mode === "draft" ? { ...prev, draft: { ...prev.draft, ...changes } } : prev,
    );

  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <input
          className="min-w-0 flex-1 basis-64 rounded-full border border-slate-200 bg-white px-4 py-2 text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") void parse();
          }}
          placeholder="예: 매주 월수금 아침 7시 헬스장에서 운동"
          maxLength={500}
          // disabled 로 두면 포커스를 잃고 접근성 트리에서도 빠진다
          readOnly={parsing}
          aria-label="자연어로 일정 입력"
        />

        {parsing ? (
          <button
            type="button"
            onClick={stop}
            className="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
          >
            취소
          </button>
        ) : (
          <button
            type="button"
            onClick={() => void parse()}
            disabled={!text.trim()}
            className="rounded-full bg-brand-500 px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
          >
            AI로 추가
          </button>
        )}

        <button
          type="button"
          onClick={() => setManual((v) => !v)}
          aria-expanded={manual}
          className="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
        >
          직접 입력
        </button>
      </div>

      {parsing && (
        <>
          <p role="status" aria-live="polite" className="text-sm text-slate-500">
            {STEPS[step]}
          </p>
          <DraftSkeleton />
        </>
      )}

      {state.mode === "error" && (
        <div className="flex flex-col items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
          <p>{state.message}</p>
          {state.needsKey ? (
            <Link
              to="/settings?focus=openai-key"
              className="rounded-md bg-brand-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-600"
            >
              설정에서 키 등록하기
            </Link>
          ) : (
            <button
              type="button"
              onClick={() => {
                setManual(true);
                setState({ mode: "idle" });
              }}
              className="rounded-md border border-red-200 bg-white px-3 py-1.5 text-xs text-red-600 hover:bg-red-100"
            >
              직접 입력하기
            </button>
          )}
        </div>
      )}

      {(state.mode === "draft" || state.mode === "saving") && (
        <ParsedDraftCard
          draft={state.draft}
          questions={state.questions}
          knownCategories={knownCategories}
          saving={state.mode === "saving"}
          onChange={patch}
          onSave={() => void save()}
          onDiscard={() => setState({ mode: "idle" })}
        />
      )}

      {manual && (
        <ScheduleInputForm
          onSubmit={onCreate}
          knownCategories={knownCategories}
          defaultDate={defaultDate}
          // 문장을 못 읽었을 때 친 내용을 버리지 않는다
          initialTitle={state.mode === "error" ? text.trim() : undefined}
        />
      )}
    </section>
  );
}

/** 답의 윤곽이 채워지는 것을 보게 한다. 스피너만으로는 몇 초가 길다 */
function DraftSkeleton() {
  return (
    <div className="flex animate-pulse flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5">
      <div className="h-8 w-2/3 rounded-md bg-slate-100" />
      <div className="flex gap-3">
        <div className="h-8 flex-1 rounded-md bg-slate-100" />
        <div className="h-8 flex-1 rounded-md bg-slate-100" />
      </div>
      <div className="h-16 rounded-xl bg-slate-100" />
    </div>
  );
}
