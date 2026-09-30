import { useEffect, useState } from "react";
import type { FormVariant, ScheduleForm } from "../../domain/scheduleForm";
import { emptyForm, formToCreateRequest, formValidationError } from "../../domain/scheduleForm";
import type { ScheduleCreateRequest } from "../../types/schedule";
import { toLocalDate } from "../../utils/datetime";
import ScheduleFields from "../schedule/ScheduleFields";

interface Props {
  /** 저장에 성공했는지 돌려준다. 실패하면 입력값을 지우지 않는다 */
  onSubmit: (body: ScheduleCreateRequest) => Promise<boolean>;
  knownTags: string[];
  /** 달력에서 날짜를 고른 상태면 시작일자를 그 날짜로 채워준다 */
  defaultDate?: Date | null;
  /** AI가 문장을 못 읽었을 때 친 문장을 제목으로 넘겨받는다 */
  initialTitle?: string;
  disabled?: boolean;
  /** 어느 탭의 폼인지. 날짜를 요구할지가 갈린다 */
  variant?: FormVariant;
  /** 이미 액자 안에 들어가 있을 때. 제 테두리와 바탕을 벗는다 */
  flat?: boolean;
}

/**
 * 리스트는 "언젠가 할 일"을 적는 곳이라 날짜 칸을 미리 채우지 않는다.
 * emptyForm 이 잡아 둔 오늘과 내일을 여기서 다시 비운다
 */
const undated = (variant: FormVariant) =>
  variant === "list" ? { startDate: "", startTime: "", endDate: "" } : {};

export default function ScheduleInputForm({
  onSubmit,
  knownTags,
  defaultDate,
  initialTitle,
  disabled,
  variant = "calendar",
  flat,
}: Props) {
  const [form, setForm] = useState<ScheduleForm>(() => ({
    ...emptyForm(defaultDate),
    ...undated(variant),
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
      setForm({ ...emptyForm(defaultDate), ...undated(variant) });
    }
  };

  return (
    <form
      className={`flex flex-col gap-3.5 ${
        flat ? "" : "rounded-2xl border border-line bg-surface p-5 shadow-xs"
      }`}
      onSubmit={handleSubmit}
    >
      <ScheduleFields
        value={form}
        onChange={change}
        knownTags={knownTags}
        idPrefix="manual"
      />

      {problem && <p className="text-sm text-danger">{problem}</p>}

      <div className="flex justify-end">
        <button
          type="submit"
          className="rounded-md bg-accent px-5 py-2 font-semibold text-canvas shadow-xs transition-colors hover:bg-ink focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line disabled:cursor-not-allowed disabled:opacity-50"
          disabled={disabled || problem !== null}
        >
          일정 추가
        </button>
      </div>
    </form>
  );
}

