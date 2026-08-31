import { useCallback, useRef, useState } from "react";
import { PARSE_QUESTION_FIELD } from "../../constants/parseQuestions";
import type { Draft, DraftFieldId } from "../../domain/draft";
import { draftValidationError } from "../../domain/draft";
import type { ParseQuestion } from "../../types/parse";
import QuestionChips from "./QuestionChips";
import RecurrenceEditor from "../RecurrenceEditor";

interface Props {
  draft: Draft;
  questions: ParseQuestion[];
  knownCategories: string[];
  saving: boolean;
  onChange: (patch: Partial<Draft>) => void;
  onSave: () => void;
  onDiscard: () => void;
}

const INPUT =
  "w-full rounded-md border bg-white px-2.5 py-2 text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100";
const LABEL = "text-xs font-semibold tracking-wide text-slate-500";
const HIGHLIGHT_MS = 1200;

/** 저장 전 마지막 확인. 미리보기가 아니라 고칠 수 있는 폼이다 */
export default function ParsedDraftCard({
  draft,
  questions,
  knownCategories,
  saving,
  onChange,
  onSave,
  onDiscard,
}: Props) {
  const [dismissed, setDismissed] = useState<ParseQuestion[]>([]);
  const [flashing, setFlashing] = useState<DraftFieldId | null>(null);
  const fields = useRef<Partial<Record<DraftFieldId, HTMLElement | null>>>({});

  const open = questions.filter((q) => !dismissed.includes(q));
  const asking = (field: DraftFieldId) =>
    open.some((q) => PARSE_QUESTION_FIELD[q] === field);

  const register = useCallback(
    (id: DraftFieldId) => (el: HTMLElement | null) => {
      fields.current[id] = el;
    },
    [],
  );

  /** 질문 하나에 답하는 것이 한 동작이 되도록 이동·포커스·강조를 함께 한다 */
  const focusField = (field: DraftFieldId) => {
    const el = fields.current[field];
    el?.scrollIntoView({ block: "nearest" });
    el?.focus();
    setFlashing(field);
    window.setTimeout(() => setFlashing(null), HIGHLIGHT_MS);
  };

  /** 칸을 고치면 그 칸을 묻던 질문은 사라진다 */
  const change = (patch: Partial<Draft>) => {
    const touched = Object.keys(patch) as DraftFieldId[];
    setDismissed((prev) => [
      ...prev,
      ...open.filter((q) => touched.includes(PARSE_QUESTION_FIELD[q])),
    ]);
    onChange(patch);
  };

  const border = (field: DraftFieldId) =>
    flashing === field
      ? "border-amber-400 ring-2 ring-amber-300"
      : asking(field)
        ? "border-amber-300"
        : "border-slate-200";

  const problem = draftValidationError(draft);

  return (
    <div className="flex flex-col gap-3.5 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
      <QuestionChips
        questions={open}
        onFocusField={focusField}
        onDismiss={(q) => setDismissed((prev) => [...prev, q])}
      />

      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor="draft-title">
          제목
        </label>
        <input
          id="draft-title"
          ref={register("title")}
          className={`${INPUT} ${border("title")}`}
          value={draft.title}
          onChange={(e) => change({ title: e.target.value })}
          maxLength={200}
        />
      </div>

      <div className="flex flex-wrap gap-3">
        <div className="flex min-w-0 flex-1 basis-44 flex-col gap-1.5">
          <label className={LABEL} htmlFor="draft-startAt">
            시작
          </label>
          <input
            id="draft-startAt"
            ref={register("startAt")}
            type="datetime-local"
            className={`${INPUT} ${border("startAt")}`}
            value={draft.startAt}
            onChange={(e) => change({ startAt: e.target.value })}
          />
        </div>

        <div className="flex min-w-0 flex-1 basis-44 flex-col gap-1.5">
          <label className={LABEL} htmlFor="draft-endAt">
            종료 (선택)
          </label>
          <input
            id="draft-endAt"
            ref={register("endAt")}
            type="datetime-local"
            className={`${INPUT} ${border("endAt")}`}
            value={draft.endAt}
            onChange={(e) => change({ endAt: e.target.value })}
          />
        </div>

        <div className="flex min-w-0 flex-1 basis-44 flex-col gap-1.5">
          <label className={LABEL} htmlFor="draft-category">
            분류 (선택)
          </label>
          <input
            id="draft-category"
            ref={register("category")}
            className={`${INPUT} ${border("category")}`}
            list="draft-category-options"
            value={draft.category}
            onChange={(e) => change({ category: e.target.value })}
            maxLength={50}
          />
          <datalist id="draft-category-options">
            {knownCategories.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor="draft-place">
          장소
        </label>
        <input
          id="draft-place"
          ref={register("place")}
          className={`${INPUT} ${border("place")}`}
          value={draft.place}
          onChange={(e) => change({ place: e.target.value })}
          placeholder="예: 헬스장"
        />
        {/* 저장할 칸이 아직 없다. 조용히 버리면 저장된 줄 안다 */}
        <span className="text-xs text-slate-400">
          장소는 아직 저장되지 않습니다.
        </span>
        <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-600 select-none">
          <input
            type="checkbox"
            className="size-4 cursor-pointer accent-brand-500"
            checked={draft.keepPlaceInDescription}
            onChange={(e) =>
              onChange({ keepPlaceInDescription: e.target.checked })
            }
            disabled={!draft.place.trim()}
          />
          장소를 메모에 남기기
        </label>
      </div>

      <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-600 select-none">
        <input
          type="checkbox"
          className="size-4 cursor-pointer accent-brand-500"
          checked={draft.allDay}
          onChange={(e) => onChange({ allDay: e.target.checked })}
        />
        하루 종일
      </label>

      <RecurrenceEditor
        value={draft}
        onChange={change}
        weekdayRef={register("byWeekday")}
        endsOnRef={register("endsOn")}
        highlight={{
          byWeekday: flashing === "byWeekday",
          endsOn: flashing === "endsOn" || asking("endsOn"),
        }}
      />

      {problem && <p className="text-sm text-red-600">{problem}</p>}

      <div className="flex items-center justify-end gap-2">
        <button
          type="button"
          onClick={onDiscard}
          className="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
        >
          취소
        </button>
        <button
          type="button"
          onClick={onSave}
          disabled={saving || problem !== null}
          className="rounded-md bg-brand-500 px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {saving ? "저장 중…" : "이대로 저장"}
        </button>
      </div>
    </div>
  );
}
