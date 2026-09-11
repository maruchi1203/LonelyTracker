import { useState } from "react";
import {
  type FormFieldId,
  type FormFreq,
  type FormKind,
  type ScheduleForm,
} from "../../domain/scheduleForm";
import type { SchedulePriority, Weekday } from "../../types/schedule";

interface Props {
  value: ScheduleForm;
  onChange: (patch: Partial<ScheduleForm>) => void;
  knownTags: string[];
  /** 두 폼이 한 화면에 뜰 수 있어 label 이 엉뚱한 칸을 가리키지 않게 한다 */
  idPrefix: string;
  /** AI 되물음이 칸을 짚을 수 있도록 */
  fieldRef?: (id: FormFieldId) => (el: HTMLElement | null) => void;
  /** 되물음이 가리키는 칸의 테두리 */
  decorate?: (id: FormFieldId) => string;
}

const INPUT =
  "w-full rounded-md border bg-white px-2.5 py-2 text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100";
const LABEL = "text-xs font-semibold tracking-wide text-slate-500";
const HINT = "text-xs text-slate-400";

/** 폼을 가르는 종류. 고른 것에 따라 아래 칸이 바뀐다 */
const KINDS: { value: FormKind; label: string; hint: string }[] = [
  { value: "simple", label: "간단", hint: "언제 할지만 적습니다" },
  { value: "period", label: "기간", hint: "시작과 끝, 마감이 있는 일입니다" },
  { value: "repeat", label: "반복", hint: "되풀이하는 일입니다" },
];

const FREQ: { value: FormFreq; label: string; ready: boolean }[] = [
  { value: "DAILY", label: "매일", ready: true },
  { value: "WEEKLY", label: "매주", ready: true },
  { value: "MONTHLY", label: "매월", ready: false },
];

const WEEKDAYS: { value: Weekday; label: string }[] = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" },
];

/** MoSCoW. 값을 비우면 아직 안 정한 것이고, 정렬에서는 COULD 로 본다 */
const PRIORITIES: { value: SchedulePriority; label: string; hint: string }[] = [
  { value: "MUST", label: "필수", hint: "반드시 해야 합니다" },
  { value: "SHOULD", label: "권장", hint: "하는 편이 좋습니다" },
  { value: "COULD", label: "선택", hint: "여유가 되면 합니다" },
  { value: "WONT", label: "보류", hint: "안 하기로 했습니다. 달력에서 빠집니다" },
];

const TOGGLE = "rounded-md border px-3 py-1 text-sm transition-colors";
const TOGGLE_ON = "border-brand-500 bg-brand-500 text-white";
const TOGGLE_OFF = "border-slate-200 text-slate-600 hover:bg-brand-50";

export default function ScheduleFields({
  value: form,
  onChange,
  knownTags,
  idPrefix,
  fieldRef,
  decorate,
}: Props) {
  const id = (name: string) => `${idPrefix}-${name}`;
  const ref = (name: FormFieldId) => fieldRef?.(name);
  const box = (name: FormFieldId) =>
    `${INPUT} ${decorate?.(name) ?? "border-slate-200"}`;

  const [tagDraft, setTagDraft] = useState("");

  /** 같은 태그를 두 번 넣지 않는다 */
  const addTag = (raw: string) => {
    const name = raw.trim();
    setTagDraft("");
    if (!name || form.tags.includes(name)) return;
    onChange({ tags: [...form.tags, name] });
  };

  const toggleWeekday = (day: Weekday) =>
    onChange({
      byWeekday: form.byWeekday.includes(day)
        ? form.byWeekday.filter((d) => d !== day)
        : [...form.byWeekday, day],
    });

  return (
    <div className="flex flex-col gap-3.5">
      {/* 1. 종류 — 아래 칸이 여기서 갈린다 */}
      <div className="flex flex-col gap-1.5">
        <span className={LABEL}>종류 *</span>
        <div className="flex items-center gap-1.5">
          {KINDS.map(({ value, label, hint }) => (
            <button
              key={value}
              type="button"
              title={hint}
              onClick={() => onChange({ kind: value })}
              aria-pressed={form.kind === value}
              className={`${TOGGLE} ${form.kind === value ? TOGGLE_ON : TOGGLE_OFF}`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* 2. 제목 */}
      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor={id("title")}>
          제목 *
        </label>
        <input
          id={id("title")}
          ref={ref("title")}
          className={box("title")}
          value={form.title}
          onChange={(e) => onChange({ title: e.target.value })}
          placeholder="무엇을 할 계획인가요?"
          maxLength={200}
        />
      </div>

      {/* 3. 우선순위 — 안 고르면 "선택"이 눌린 것처럼 보이되 값은 비어 있다 */}
      <div className="flex flex-col gap-1.5">
        <span className={LABEL}>우선순위 *</span>
        <div ref={ref("priority")} className="flex flex-wrap items-center gap-1.5">
          {PRIORITIES.map(({ value, label, hint }) => {
            const on = (form.priority || "COULD") === value;
            return (
              <button
                key={value}
                type="button"
                title={hint}
                onClick={() => onChange({ priority: value })}
                aria-pressed={on}
                className={`${TOGGLE} ${on ? TOGGLE_ON : TOGGLE_OFF}`}
              >
                {label}
              </button>
            );
          })}
        </div>
      </div>

      {/* 4. 태그 */}
      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor={id("tags")}>
          태그
        </label>

        {form.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {form.tags.map((tag) => (
              <button
                key={tag}
                type="button"
                onClick={() => onChange({ tags: form.tags.filter((t) => t !== tag) })}
                aria-label={`태그 ${tag} 빼기`}
                className="rounded-full border border-brand-200 bg-brand-50 px-2.5 py-1 text-xs text-brand-700 hover:bg-brand-100"
              >
                {tag} ×
              </button>
            ))}
          </div>
        )}

        {/* 자유 입력. 후보에 없는 이름도 쓸 수 있다 */}
        <input
          id={id("tags")}
          ref={ref("tags")}
          className={box("tags")}
          list={id("tag-options")}
          value={tagDraft}
          onChange={(e) => {
            // 쉼표로 끝내면 그 자리에서 확정한다
            if (e.target.value.endsWith(",")) addTag(e.target.value.slice(0, -1));
            else setTagDraft(e.target.value);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              // 폼 전체가 제출되지 않게 막는다
              e.preventDefault();
              addTag(tagDraft);
            }
          }}
          onBlur={() => addTag(tagDraft)}
          placeholder="예: 육체 — Enter 로 추가"
          maxLength={50}
          autoComplete="off"
        />
        <datalist id={id("tag-options")}>
          {knownTags
            .filter((t) => !form.tags.includes(t))
            .map((t) => (
              <option key={t} value={t} />
            ))}
        </datalist>
      </div>

      {/* 5. 시작 — 세 종류 모두 쓴다 */}
      <div className="flex flex-wrap gap-3">
        <div className="flex min-w-0 flex-1 basis-40 flex-col gap-1.5">
          <label className={LABEL} htmlFor={id("startDate")}>
            시작일자
          </label>
          <input
            id={id("startDate")}
            ref={ref("startDate")}
            type="date"
            className={box("startDate")}
            value={form.startDate}
            onChange={(e) => onChange({ startDate: e.target.value })}
          />
        </div>

        <div className="flex min-w-0 flex-1 basis-40 flex-col gap-1.5">
          <label className={LABEL} htmlFor={id("startTime")}>
            시작시각
          </label>
          <input
            id={id("startTime")}
            ref={ref("startTime")}
            type="time"
            className={box("startTime")}
            value={form.startTime}
            onChange={(e) => onChange({ startTime: e.target.value })}
          />
          <span className={HINT}>비우면 하루 종일로 저장됩니다.</span>
        </div>
      </div>

      {/* 6. 기간 — 끝과 마감 */}
      {form.kind === "period" && (
        <>
          <div className="flex flex-wrap gap-3">
            <div className="flex min-w-0 flex-1 basis-40 flex-col gap-1.5">
              <label className={LABEL} htmlFor={id("endDate")}>
                종료일자
              </label>
              <input
                id={id("endDate")}
                ref={ref("endDate")}
                type="date"
                className={box("endDate")}
                value={form.endDate}
                onChange={(e) => onChange({ endDate: e.target.value })}
              />
              <span className={HINT}>비우면 시작일자와 같은 날로 봅니다.</span>
            </div>

            <div className="flex min-w-0 flex-1 basis-40 flex-col gap-1.5">
              <label className={LABEL} htmlFor={id("endTime")}>
                종료시각
              </label>
              <input
                id={id("endTime")}
                ref={ref("endTime")}
                type="time"
                className={box("endTime")}
                value={form.endTime}
                onChange={(e) => onChange({ endTime: e.target.value })}
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className={LABEL} htmlFor={id("dueOn")}>
              마감기한
            </label>
            <input
              id={id("dueOn")}
              ref={ref("dueOn")}
              type="date"
              className={box("dueOn")}
              value={form.dueOn}
              onChange={(e) => onChange({ dueOn: e.target.value })}
            />
            <span className={HINT}>
              언제까지 해내야 하는지입니다. 시작일시와는 다릅니다.
            </span>
          </div>
        </>
      )}

      {/* 7. 반복 — 주기 */}
      {form.kind === "repeat" && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>주기 *</span>
          <div ref={ref("freq")} className="flex items-center gap-1.5">
            {FREQ.map(({ value: freq, label, ready }) => (
              <button
                key={freq}
                type="button"
                disabled={!ready}
                title={ready ? undefined : "준비 중입니다"}
                onClick={() => onChange({ freq })}
                aria-pressed={form.freq === freq}
                className={`${TOGGLE} disabled:cursor-not-allowed disabled:opacity-40 ${
                  form.freq === freq ? TOGGLE_ON : TOGGLE_OFF
                }`}
              >
                {label}
                {!ready && " (준비 중)"}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 8. 매주 반복요일 */}
      {form.kind === "repeat" && form.freq === "WEEKLY" && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>반복요일 *</span>
          <div
            ref={ref("byWeekday")}
            tabIndex={-1}
            className={`flex flex-wrap gap-1 rounded-md ${decorate?.("byWeekday") ?? ""}`}
          >
            {WEEKDAYS.map(({ value: day, label }) => (
              <button
                key={day}
                type="button"
                onClick={() => toggleWeekday(day)}
                aria-pressed={form.byWeekday.includes(day)}
                className={`size-8 rounded-full border text-sm transition-colors ${
                  form.byWeekday.includes(day) ? TOGGLE_ON : TOGGLE_OFF
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 9. 매월 반복일자 — 백엔드가 받기 전까지 자리만 */}
      {form.kind === "repeat" && form.freq === "MONTHLY" && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>반복일자 *</span>
          <p className="rounded-md border border-dashed border-slate-300 px-3 py-2 text-xs text-slate-400">
            매월 반복은 준비 중입니다.
          </p>
        </div>
      )}
    </div>
  );
}
