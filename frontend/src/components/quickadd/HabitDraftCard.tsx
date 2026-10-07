import { useState } from "react";
import { HABIT_QUESTION_TEXT } from "../../constants/habitQuestions";
import type {
  HabitCategory,
  HabitCreateRequest,
  HabitQuestion,
} from "../../types/habit";

/** 저장 전까지 고쳐 가는 값. 카테고리는 아직 못 고른 상태가 있다 */
export interface HabitDraftForm {
  title: string;
  categoryId: number | null;
  atTime: string;
  place: string;
  twoMinuteAction: string;
}

interface Props {
  draft: HabitDraftForm;
  questions: HabitQuestion[];
  categories: HabitCategory[];
  /** 2분 행동을 AI 가 지어냈는지. 사용자가 제 것으로 고쳐 쓰게 밝혀 둔다 */
  suggestedAction: boolean;
  /** 2분 법칙을 끄면 신호 칸이 통째로 내려간다 */
  twoMinuteRule: boolean;
  saving: boolean;
  onChange: (patch: Partial<HabitDraftForm>) => void;
  onSave: () => void;
  onDiscard: () => void;
}

const FIELD =
  "w-full rounded-md border border-line bg-surface px-2.5 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-accent focus:outline-none";
const LABEL = "text-xs font-semibold text-ink-soft";
/** 아직 답을 기다리는 칸. 테두리로 어디를 봐야 하는지 가리킨다 */
const ASKED = "border-warn ring-2 ring-warn-soft";

/** 저장 전 마지막 확인. 미리보기가 아니라 고칠 수 있는 폼이다 */
export default function HabitDraftCard({
  draft,
  questions,
  categories,
  suggestedAction,
  twoMinuteRule,
  saving,
  onChange,
  onSave,
  onDiscard,
}: Props) {
  const [dismissed, setDismissed] = useState<HabitQuestion[]>([]);

  const open = questions.filter((q) => !dismissed.includes(q));
  const asking = (q: HabitQuestion) => open.includes(q);

  /** 카테고리를 못 고르면 넣을 자리가 없다. 나머지 빈 칸은 저장을 막지 않는다 */
  const blocked = draft.categoryId === null || !draft.title.trim();

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!blocked && !saving) onSave();
      }}
      className="flex flex-col gap-3 rounded-2xl border border-line bg-surface p-4"
    >
      {open.length > 0 && (
        <ul className="flex list-none flex-col gap-1.5 p-0">
          {open.map((q) => (
            <li
              key={q}
              className="flex items-start justify-between gap-2 rounded-xl border border-warn bg-warn-soft px-3 py-2"
            >
              <span className="text-sm text-warn">{HABIT_QUESTION_TEXT[q]}</span>
              <button
                type="button"
                aria-label="이 질문 닫기"
                onClick={() => setDismissed((prev) => [...prev, q])}
                className="shrink-0 text-warn"
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      )}

      <label className="flex flex-col gap-1">
        <span className={LABEL}>습관 *</span>
        <input
          value={draft.title}
          onChange={(e) => onChange({ title: e.target.value })}
          maxLength={200}
          className={`${FIELD} ${asking("TOO_VAGUE") ? ASKED : ""}`}
        />
      </label>

      <label className="flex flex-col gap-1">
        <span className={LABEL}>카테고리 *</span>
        <select
          value={draft.categoryId ?? ""}
          onChange={(e) =>
            onChange({
              categoryId: e.target.value === "" ? null : Number(e.target.value),
            })
          }
          className={`${FIELD} ${asking("CATEGORY") ? ASKED : ""}`}
        >
          {/* 서버가 못 맞췄으면 비어 있다. 고르기 전에는 저장이 막힌다 */}
          <option value="">고르지 않음</option>
          {categories.map((one) => (
            <option key={one.id} value={one.id}>
              {one.name}
            </option>
          ))}
        </select>
      </label>

      {twoMinuteRule && (
        <>
          <div className="flex flex-wrap gap-2">
            <label className="flex min-w-0 flex-1 basis-32 flex-col gap-1">
              <span className={LABEL}>언제</span>
              <input
                value={draft.atTime}
                onChange={(e) => onChange({ atTime: e.target.value })}
                placeholder="예: 퇴근 후"
                maxLength={100}
                className={`${FIELD} ${asking("CUE_TIME") ? ASKED : ""}`}
              />
            </label>

            <label className="flex min-w-0 flex-1 basis-32 flex-col gap-1">
              <span className={LABEL}>어디서</span>
              <input
                value={draft.place}
                onChange={(e) => onChange({ place: e.target.value })}
                placeholder="예: 거실"
                maxLength={200}
                className={`${FIELD} ${asking("PLACE") ? ASKED : ""}`}
              />
            </label>
          </div>

          <label className="flex flex-col gap-1">
            <span className={`${LABEL} flex items-center gap-1.5`}>
              2분 행동
              {/*
                지어낸 값을 사용자가 적은 값처럼 보이게 두면 안 된다.
                제 하루에 맞게 고치는 것이 이 칸의 쓸모다
              */}
              {suggestedAction && (
                <span className="rounded-full bg-accent-soft px-1.5 py-0.5 text-[11px] font-normal text-accent">
                  AI 제안 · 고쳐 쓰세요
                </span>
              )}
            </span>
            <input
              value={draft.twoMinuteAction}
              onChange={(e) => onChange({ twoMinuteAction: e.target.value })}
              placeholder="예: 매트 깔기"
              maxLength={200}
              className={`${FIELD} ${asking("TWO_MINUTE") ? ASKED : ""}`}
            />
          </label>
        </>
      )}

      <div className="flex justify-end gap-1.5">
        <button
          type="button"
          onClick={onDiscard}
          className="rounded-md border border-line px-3 py-2 text-sm text-ink-soft hover:bg-surface-soft"
        >
          버리기
        </button>
        <button
          type="submit"
          disabled={blocked || saving}
          title={draft.categoryId === null ? "카테고리를 골라 주세요" : undefined}
          className="rounded-md bg-accent px-3 py-2 text-sm font-semibold text-canvas transition-colors hover:bg-ink disabled:cursor-not-allowed disabled:opacity-50"
        >
          {saving ? "저장 중…" : "저장"}
        </button>
      </div>
    </form>
  );
}

/** 저장할 수 있는 모양으로 바꾼다. 빈 칸은 보내지 않는다 */
export function draftToCreateRequest(
  draft: HabitDraftForm,
): HabitCreateRequest | null {
  if (draft.categoryId === null || !draft.title.trim()) return null;

  return {
    title: draft.title.trim(),
    categoryId: draft.categoryId,
    atTime: draft.atTime.trim() || undefined,
    place: draft.place.trim() || undefined,
    twoMinuteAction: draft.twoMinuteAction.trim() || undefined,
  };
}
