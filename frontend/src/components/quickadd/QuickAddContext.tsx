import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { HttpError } from "../../api/http";
import { createSchedule, parseSchedule } from "../../api/schedules";
import { fetchAiProviders } from "../../api/users";
import { knownQuestions } from "../../constants/parseQuestions";
import type { FormVariant, ScheduleForm } from "../../domain/scheduleForm";
import { draftFromParsed } from "../../domain/scheduleForm";
import type { ParseQuestion } from "../../types/parse";
import type { ScheduleCreateRequest } from "../../types/schedule";

/** 카드 한 장. key 는 목록에서 지워도 안 흔들리는 자리표다 */
export interface Draft {
  key: number;
  form: ScheduleForm;
  questions: ParseQuestion[];
  saving: boolean;
}

export type QuickAddState =
  | { mode: "idle" }
  | { mode: "parsing" }
  | { mode: "drafts"; drafts: Draft[] }
  // AI 는 답했지만 초안이 없다. 오류가 아니다
  | { mode: "notice"; message: string }
  | { mode: "error"; message: string; needsKey: boolean };

/** 서버 읽기 타임아웃이 30초라 그보다 조금 뒤에 포기한다 */
const GIVE_UP_MS = 35_000;

/** 설정에 다녀오는 동안 친 문장을 잃지 않게 둘 자리 */
const DRAFT_TEXT_KEY = "quickadd-text";

/** 지금 화면이 빠른 추가에 넘겨 주는 것 */
interface Target {
  /** 달력에서 고른 날짜. 리스트처럼 날짜 개념이 없는 탭은 주지 않는다 */
  defaultDate: Date | null;
  knownTags: string[];
  /** 어느 탭의 폼인지. 날짜를 요구할지가 갈린다 */
  variant: FormVariant;
}

const NO_TARGET: Target = {
  defaultDate: null,
  knownTags: [],
  variant: "calendar",
};

type Saver = (body: ScheduleCreateRequest) => Promise<boolean>;

interface QuickAddValue extends Target {
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
  text: string;
  setText: React.Dispatch<React.SetStateAction<string>>;
  state: QuickAddState;
  setState: React.Dispatch<React.SetStateAction<QuickAddState>>;
  manual: boolean;
  setManual: React.Dispatch<React.SetStateAction<boolean>>;
  parse: () => Promise<void>;
  create: Saver;

  /** 화면이 자기 값을 걸어 두는 자리. useQuickAddTarget 이 대신 부른다 */
  claim: (target: Target, save: Saver) => void;
  release: () => void;
}

const Ctx = createContext<QuickAddValue | null>(null);

/**
 * 빠른 추가의 상태를 화면보다 위에 둔다.
 *
 * 패널을 닫거나 탭을 옮겨도 살아남아야 한다. 받아 둔 초안을 잃으면
 * 사용자가 AI 를 한 번 더 부르게 되고, 그 토큰 값은 사용자가 낸다
 */
export function QuickAddProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState(
    () => sessionStorage.getItem(DRAFT_TEXT_KEY) ?? "",
  );
  const [state, setState] = useState<QuickAddState>({ mode: "idle" });
  const [manual, setManual] = useState(false);
  const [target, setTarget] = useState<Target>(NO_TARGET);

  const abort = useRef<AbortController | null>(null);

  /**
   * 지금 화면의 저장 함수. state 가 아니라 ref 인 이유는
   * 화면이 렌더될 때마다 새로 만들어져, 의존성으로 쓰면 등록이 끝없이 돌기 때문이다
   */
  const saver = useRef<Saver | null>(null);

  // 앱을 떠날 때만 거둔다. 패널을 닫는 것은 그만두는 것이 아니다
  useEffect(() => () => abort.current?.abort(), []);

  const claim = useCallback((next: Target, save: Saver) => {
    saver.current = save;
    // 값이 그대로면 같은 객체를 돌려줘 헛렌더를 막는다
    setTarget((prev) =>
      prev.defaultDate?.getTime() === next.defaultDate?.getTime() &&
      prev.knownTags === next.knownTags &&
      prev.variant === next.variant
        ? prev
        : next,
    );
  }, []);

  const release = useCallback(() => {
    saver.current = null;
    setTarget(NO_TARGET);
  }, []);

  const parse = useCallback(async () => {
    const sentence = text.trim();
    if (!sentence) return;

    const controller = new AbortController();
    abort.current = controller;
    const giveUp = window.setTimeout(() => controller.abort(), GIVE_UP_MS);
    setState({ mode: "parsing" });

    try {
      const parsed = await parseSchedule(sentence, controller.signal);
      if (parsed.notice) {
        setState({ mode: "notice", message: parsed.notice });
        return;
      }
      setState({
        mode: "drafts",
        drafts: parsed.schedules.map((one, at) => ({
          key: at,
          form: draftFromParsed(one, target.defaultDate, target.variant),
          questions: knownQuestions(one.questions),
          saving: false,
        })),
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
        needsKey = await fetchAiProviders()
          .then((l) => !l.serverConfigured && !l.providers.some((c) => c.active))
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
  }, [text, target.defaultDate, target.variant]);

  /**
   * 화면이 걸어 둔 저장 함수를 쓴다.
   * 그 화면을 떠났으면 저장만 한다. 목록을 다시 읽는 일은 그 화면의 몫이었다
   */
  const create = useCallback<Saver>(async (body) => {
    if (saver.current) return saver.current(body);
    try {
      await createSchedule(body);
      return true;
    } catch {
      return false;
    }
  }, []);

  const value = useMemo<QuickAddValue>(
    () => ({
      ...target,
      open,
      setOpen,
      text,
      setText,
      state,
      setState,
      manual,
      setManual,
      parse,
      create,
      claim,
      release,
    }),
    [target, open, text, state, manual, parse, create, claim, release],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useQuickAdd(): QuickAddValue {
  const value = useContext(Ctx);
  if (value === null) {
    throw new Error("QuickAddProvider 안에서만 쓸 수 있습니다");
  }
  return value;
}

/**
 * 이 화면의 값을 빠른 추가에 걸어 둔다. 화면을 떠나면 스스로 걷어낸다.
 *
 * 걷어내지 않으면 떠난 화면의 저장 함수가 남아, 저장은 되는데
 * 지금 보고 있는 목록은 갱신되지 않는 일이 생긴다
 */
export function useQuickAddTarget(
  target: Target,
  save: (body: ScheduleCreateRequest) => Promise<boolean>,
) {
  const { claim, release } = useQuickAdd();
  const latest = useRef(save);
  latest.current = save;

  const { defaultDate, knownTags, variant } = target;
  useEffect(() => {
    claim({ defaultDate, knownTags, variant }, (body) => latest.current(body));
    return release;
  }, [claim, release, defaultDate, knownTags, variant]);
}
