import { useCallback, useRef, useState } from "react";
import { PARSE_QUESTION_FIELD } from "../../constants/parseQuestions";
import type { FormFieldId, ScheduleForm } from "../../domain/scheduleForm";
import { formValidationError } from "../../domain/scheduleForm";
import type { ParseQuestion } from "../../types/parse";
import ScheduleFields from "../schedule/ScheduleFields";
import QuestionChips from "./QuestionChips";

interface Props {
  draft: ScheduleForm;
  questions: ParseQuestion[];
  knownTags: string[];
  saving: boolean;
  showTwoMinute?: boolean;
  onChange: (patch: Partial<ScheduleForm>) => void;
  onSave: () => void;
  onDiscard: () => void;
}

const HIGHLIGHT_MS = 1200;

/** 저장 전 마지막 확인. 미리보기가 아니라 고칠 수 있는 폼이다 */
export default function ParsedDraftCard({
  draft,
  questions,
  knownTags,
  saving,
  showTwoMinute,
  onChange,
  onSave,
  onDiscard,
}: Props) {
  const [dismissed, setDismissed] = useState<ParseQuestion[]>([]);
  const [flashing, setFlashing] = useState<FormFieldId | null>(null);
  const fields = useRef<Partial<Record<FormFieldId, HTMLElement | null>>>({});

  const open = questions.filter((q) => !dismissed.includes(q));
  const asking = (field: FormFieldId) =>
    open.some((q) => PARSE_QUESTION_FIELD[q] === field);

  const fieldRef = useCallback(
    (id: FormFieldId) => (el: HTMLElement | null) => {
      fields.current[id] = el;
    },
    [],
  );

  /** 질문 하나에 답하는 것이 한 동작이 되도록 이동·포커스·강조를 함께 한다 */
  const focusField = (field: FormFieldId) => {
    const el = fields.current[field];
    el?.scrollIntoView({ block: "nearest" });
    el?.focus();
    setFlashing(field);
    window.setTimeout(() => setFlashing(null), HIGHLIGHT_MS);
  };

  /** 칸을 고치면 그 칸을 묻던 질문은 사라진다 */
  const change = (patch: Partial<ScheduleForm>) => {
    const touched = Object.keys(patch) as FormFieldId[];
    setDismissed((prev) => [
      ...prev,
      ...open.filter((q) => touched.includes(PARSE_QUESTION_FIELD[q])),
    ]);
    onChange(patch);
  };

  const decorate = (field: FormFieldId) =>
    flashing === field
      ? "border-amber-400 ring-2 ring-amber-300"
      : asking(field)
        ? "border-amber-300"
        : "border-slate-200";

  const problem = formValidationError(draft);

  return (
    <div className="flex flex-col gap-3.5 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
      <QuestionChips
        questions={open}
        onFocusField={focusField}
        onDismiss={(q) => setDismissed((prev) => [...prev, q])}
      />

      <ScheduleFields
        value={draft}
        onChange={change}
        knownTags={knownTags}
        idPrefix="draft"
        fieldRef={fieldRef}
        decorate={decorate}
        showTwoMinute={showTwoMinute}
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
