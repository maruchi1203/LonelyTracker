import {
  PARSE_QUESTION_FIELD,
  PARSE_QUESTION_TEXT,
} from "../../constants/parseQuestions";
import type { FormFieldId } from "../../domain/scheduleForm";
import type { ParseQuestion } from "../../types/parse";

interface Props {
  questions: ParseQuestion[];
  onFocusField: (field: FormFieldId) => void;
  onDismiss: (question: ParseQuestion) => void;
}

/** AI가 채우지 못한 칸을 묻는다. 누르면 그 입력칸으로 데려간다 */
export default function QuestionChips({
  questions,
  onFocusField,
  onDismiss,
}: Props) {
  if (questions.length === 0) return null;

  const vague = questions.includes("TOO_VAGUE");
  const fillable = questions.filter((q) => q !== "TOO_VAGUE");

  return (
    <div className="flex flex-col gap-2">
      {/* 빈칸 채우기가 아니라 행동을 다시 생각해 보자는 제안이다 */}
      {vague && (
        <div className="flex items-start justify-between gap-2 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2">
          <button
            type="button"
            onClick={() => onFocusField(PARSE_QUESTION_FIELD.TOO_VAGUE)}
            className="text-left text-sm text-amber-800 underline-offset-2 hover:underline"
          >
            {PARSE_QUESTION_TEXT.TOO_VAGUE}
          </button>
          <button
            type="button"
            aria-label="이 질문 닫기"
            className="shrink-0 text-amber-500 hover:text-amber-700"
            onClick={() => onDismiss("TOO_VAGUE")}
          >
            ×
          </button>
        </div>
      )}

      {fillable.length > 0 && (
        <ul className="flex list-none flex-wrap gap-1.5 p-0">
          {fillable.map((question) => (
            <li key={question}>
              <span className="flex items-center gap-1 rounded-full border border-amber-200 bg-amber-50 py-1 pr-1 pl-3 text-sm text-amber-800">
                <button
                  type="button"
                  onClick={() => onFocusField(PARSE_QUESTION_FIELD[question])}
                  className="underline-offset-2 hover:underline"
                >
                  {PARSE_QUESTION_TEXT[question]}
                </button>
                <button
                  type="button"
                  aria-label="이 질문 닫기"
                  className="rounded-full px-1.5 text-amber-500 hover:text-amber-700"
                  onClick={() => onDismiss(question)}
                >
                  ×
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
