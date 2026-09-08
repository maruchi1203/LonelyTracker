import { useEffect, useState } from "react";
import type { FormVariant, ScheduleForm } from "../domain/scheduleForm";
import { emptyForm, formToCreateRequest, formValidationError } from "../domain/scheduleForm";
import type { ScheduleCreateRequest } from "../types/schedule";
import { toLocalDate } from "../utils/datetime";
import ScheduleFields, { type ParentOption } from "./schedule/ScheduleFields";

interface Props {
  /** 저장에 성공했는지 돌려준다. 실패하면 입력값을 지우지 않는다 */
  onSubmit: (body: ScheduleCreateRequest) => Promise<boolean>;
  knownTags: string[];
  /** 달력에서 날짜를 고른 상태면 시작일자를 그 날짜로 채워준다 */
  defaultDate?: Date | null;
  /** AI가 문장을 못 읽었을 때 친 문장을 제목으로 넘겨받는다 */
  initialTitle?: string;
  showTwoMinute?: boolean;
  disabled?: boolean;
  /** 어느 탭의 폼인지. 칸 구성과 검증이 함께 갈린다 */
  variant?: FormVariant;
  parentOptions?: ParentOption[];
}

export default function ScheduleInputForm({
  onSubmit,
  knownTags,
  defaultDate,
  initialTitle,
  showTwoMinute,
  disabled,
  variant = "calendar",
  parentOptions,
}: Props) {
  const [form, setForm] = useState<ScheduleForm>(() => ({
    ...emptyForm(defaultDate),
    // 리스트는 날짜를 안 정한 채로 적는 곳이라 시작일자를 미리 채우지 않는다
    ...(variant === "list" ? { startDate: "", startTime: "" } : {}),
    title: initialTitle ?? "",
  }));

  // 고른 날짜가 바뀌면 시작일자만 다시 잡는다. 나머지 입력은 그대로 둔다
  useEffect(() => {
    if (variant === "list") return;

    setForm((prev) => ({
      ...prev,
      startDate: toLocalDate(defaultDate ?? new Date()),
    }));
  }, [defaultDate, variant]);

  const change = (patch: Partial<ScheduleForm>) =>
    setForm((prev) => ({ ...prev, ...patch }));

  const problem = formValidationError(form, variant);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); // 폼 기본 동작(페이지 새로고침)을 막는다
    if (problem) return;

    const created = await onSubmit(formToCreateRequest(form));

    // 실패했는데 입력을 지우면 사용자가 처음부터 다시 써야 한다
    if (created) {
      setForm({
        ...emptyForm(defaultDate),
        ...(variant === "list" ? { startDate: "", startTime: "" } : {}),
      });
    }
  };

  return (
    <form
      className="flex flex-col gap-3.5 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs"
      onSubmit={handleSubmit}
    >
      <ScheduleFields
        value={form}
        onChange={change}
        knownTags={knownTags}
        idPrefix="manual"
        showTwoMinute={showTwoMinute}
        variant={variant}
        parentOptions={parentOptions}
      />

      {problem && <p className="text-sm text-red-600">{problem}</p>}

      <div className="flex justify-end">
        <button
          type="submit"
          className="rounded-md bg-brand-500 px-5 py-2 font-semibold text-white shadow-xs transition-colors hover:bg-brand-600 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={disabled || problem !== null}
        >
          일정 추가
        </button>
      </div>
    </form>
  );
}

