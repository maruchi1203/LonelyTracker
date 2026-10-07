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
import { parseHabit } from "../../api/habits";
import { createSchedule, parseSchedule } from "../../api/schedules";
import { fetchAiProviders } from "../../api/users";
import { knownHabitQuestions } from "../../constants/habitQuestions";
import { knownQuestions } from "../../constants/parseQuestions";
import type { FormVariant, ScheduleForm } from "../../domain/scheduleForm";
import { draftFromParsed } from "../../domain/scheduleForm";
import type { ParseQuestion } from "../../types/parse";
import type {
  HabitCategory,
  HabitCreateRequest,
  HabitQuestion,
} from "../../types/habit";
import type { ScheduleCreateRequest } from "../../types/schedule";
import type { HabitDraftForm } from "./HabitDraftCard";

/** 카드 한 장. key 는 목록에서 지워도 안 흔들리는 자리표다 */
export interface Draft {
  key: number;
  form: ScheduleForm;
  questions: ParseQuestion[];
  saving: boolean;
}

/**
 * 습관 초안 카드 한 장.
 *
 * 일정 초안과 한 타입에 담지 않는다. 담을 값이 다르고(카테고리·2분 행동),
 * 되물을 것도 달라 어느 한쪽 칸이 늘 비어 있게 된다
 */
export interface HabitDraftState {
  key: number;
  form: HabitDraftForm;
  questions: HabitQuestion[];
  /** 2분 행동을 AI 가 지어냈는지. 카드가 "제안"이라고 밝힌다 */
  suggestedAction: boolean;
  saving: boolean;
}

export type QuickAddState =
  | { mode: "idle" }
  | { mode: "parsing" }
  | { mode: "drafts"; drafts: Draft[] }
  | { mode: "habitDrafts"; drafts: HabitDraftState[] }
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
  /** 습관일지가 걸어 둔 카테고리. 일정 탭은 반려줄 것이 없어 마다 */
  categories: HabitCategory[];
  /** 2분 법칙을 쓰기로 했는지. 습관 폼의 신호 칸이 여기에 달린다 */
  twoMinuteRule: boolean;
}

/**
 * 일정 탭이 카테고리 자리에 넣는 값.
 * 자리마다 [] 를 새로 만들면 렌더마다 다른 객제가 되어 claim 이 끝없이 도다
 */
export const NO_CATEGORIES: HabitCategory[] = [];

const NO_TARGET: Target = {
  defaultDate: null,
  knownTags: [],
  variant: "calendar",
  categories: NO_CATEGORIES,
  twoMinuteRule: true,
};

/**
 * 지금 화면이 맡은 저장. 탭에 따라 한쪽만 채워진다.
 *
 * 하나로 합치지 않는 까닭은 일정과 습관이 서로 다른 것이기 때문이다.
 * 몸통을 합치면 부르는 자리에서 캡스팅이 들고, 그러면 타입이 지킬 것이 없어진다
 */
interface Savers {
  schedule?: (body: ScheduleCreateRequest) => Promise<boolean>;
  habit?: (body: HabitCreateRequest) => Promise<boolean>;
}

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
  /** 읽기를 그만둔다. 서버는 이미 부른 뒤라 쓴 토큰은 돌아오지 않는다 */
  stop: () => void;
  create: (body: ScheduleCreateRequest) => Promise<boolean>;
  /** 습관을 만들어 주는 쪽. 습관일지가 아닌 탭에서는 뛰어봤도 거짓이다 */
  createHabit: (body: HabitCreateRequest) => Promise<boolean>;

  /** 화면이 자기 값을 걸어 두는 자리. useQuickAddTarget 이 대신 부른다 */
  claim: (target: Target, savers: Savers) => void;
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
   * 왜 끊겼는지. 사용자가 그만둔 것과 시간이 다 된 것을 갈라야 한다.
   * 둘 다 같은 abort 로 끝나서 신호만으로는 구별되지 않는다
   */
  const cancelled = useRef(false);

  /**
   * 지금 화면의 저장 함수. state 가 아니라 ref 인 이유는
   * 화면이 렌더될 때마다 새로 만들어져, 의존성으로 쓰면 등록이 끝없이 돌기 때문이다
   */
  const savers = useRef<Savers>({});

  // 앱을 떠날 때만 거둔다. 패널을 닫는 것은 그만두는 것이 아니다
  useEffect(() => () => abort.current?.abort(), []);

  const claim = useCallback((next: Target, nextSavers: Savers) => {
    savers.current = nextSavers;
    // 값이 그대로면 같은 객체를 돌려줘 헛렌더를 막는다
    setTarget((prev) =>
      prev.defaultDate?.getTime() === next.defaultDate?.getTime() &&
      prev.knownTags === next.knownTags &&
      prev.variant === next.variant &&
      prev.categories === next.categories &&
      prev.twoMinuteRule === next.twoMinuteRule
        ? prev
        : next,
    );
  }, []);

  const release = useCallback(() => {
    savers.current = {};
    setTarget(NO_TARGET);
  }, []);

  const parse = useCallback(async () => {
    const sentence = text.trim();
    if (!sentence) return;

    const controller = new AbortController();
    abort.current = controller;
    cancelled.current = false;
    const giveUp = window.setTimeout(() => controller.abort(), GIVE_UP_MS);
    setState({ mode: "parsing" });

    try {
      // 탭마다 읽는 것이 다르다. 습관 탭에서 일정 프롬프트를 쓰면 카테고리가 안 온다
      if (target.variant === "habit") {
        const read = await parseHabit(sentence, controller.signal);
        if (read.notice) {
          setState({ mode: "notice", message: read.notice });
          return;
        }
        setState({
          mode: "habitDrafts",
          drafts: read.habits.map((one, at) => ({
            key: at,
            form: {
              title: one.title,
              categoryId: one.categoryId ?? null,
              atTime: one.atTime ?? "",
              place: one.place ?? "",
              twoMinuteAction: one.twoMinuteAction ?? "",
            },
            questions: knownHabitQuestions(one.questions),
            suggestedAction: one.suggestedAction,
            saving: false,
          })),
        });
        sessionStorage.removeItem(DRAFT_TEXT_KEY);
        return;
      }

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
        // 사용자가 그만둔 것이면 stop 이 이미 입력줄로 되돌려 놓았다
        if (!cancelled.current) {
          setState({
            mode: "error",
            message: "응답이 너무 늦습니다. 직접 입력해 주세요.",
            needsKey: false,
          });
        }
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

  const stop = useCallback(() => {
    cancelled.current = true;
    abort.current?.abort();
    setState({ mode: "idle" });
  }, []);

  /**
   * 화면이 걸어 둔 저장 함수를 쓴다.
   * 그 화면을 떠났으면 저장만 한다. 목록을 다시 읽는 일은 그 화면의 몫이었다
   */
  const create = useCallback(async (body: ScheduleCreateRequest) => {
    if (savers.current.schedule) return savers.current.schedule(body);
    try {
      await createSchedule(body);
      return true;
    } catch {
      return false;
    }
  }, []);

  /**
   * 습관은 걸어 둔 화면이 없으면 만들지 않는다.
   * 일정과 달리 어느 카테고리에 넣을지가 그 화면에만 있어서, 떠난 뒤에 저장하면
   * 사라진 카테고리에 넣으려 들 수 있다
   */
  const createHabit = useCallback(async (body: HabitCreateRequest) => {
    if (!savers.current.habit) return false;
    return savers.current.habit(body);
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
      stop,
      create,
      createHabit,
      claim,
      release,
    }),
    [
      target,
      open,
      text,
      state,
      manual,
      parse,
      stop,
      create,
      createHabit,
      claim,
      release,
    ],
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
export function useQuickAddTarget(target: Target, savers: Savers) {
  const { claim, release } = useQuickAdd();

  /*
   * 화면이 렌더될 때마다 새로 만들어지는 함수들이다.
   * 의존성으로 쓰면 들여넣기가 끝없이 도는다
   */
  const latest = useRef(savers);
  latest.current = savers;

  const { defaultDate, knownTags, variant, categories, twoMinuteRule } =
    target;
  useEffect(() => {
    claim(
      { defaultDate, knownTags, variant, categories, twoMinuteRule },
      {
        schedule: (body) => latest.current.schedule?.(body) ?? skip(),
        habit: (body) => latest.current.habit?.(body) ?? skip(),
      },
    );
    return release;
  }, [
    claim,
    release,
    defaultDate,
    knownTags,
    variant,
    categories,
    twoMinuteRule,
  ]);
}

/** 그 탭이 맡지 않는 종류를 써 보려 했을 때. 조용하세 실패로 둔다 */
function skip(): Promise<boolean> {
  return Promise.resolve(false);
}
