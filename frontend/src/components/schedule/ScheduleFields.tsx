import type { FormFieldId, FormFreq, ScheduleForm } from "../../domain/scheduleForm";
import type { Weekday } from "../../types/schedule";

interface Props {
  value: ScheduleForm;
  onChange: (patch: Partial<ScheduleForm>) => void;
  knownCategories: string[];
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

const TOGGLE = "rounded-md border px-3 py-1 text-sm transition-colors";
const TOGGLE_ON = "border-brand-500 bg-brand-500 text-white";
const TOGGLE_OFF = "border-slate-200 text-slate-600 hover:bg-brand-50";

export default function ScheduleFields({
  value: form,
  onChange,
  knownCategories,
  idPrefix,
  fieldRef,
  decorate,
}: Props) {
  const id = (name: string) => `${idPrefix}-${name}`;
  const ref = (name: FormFieldId) => fieldRef?.(name);
  const box = (name: FormFieldId) =>
    `${INPUT} ${decorate?.(name) ?? "border-slate-200"}`;

  const toggleWeekday = (day: Weekday) =>
    onChange({
      byWeekday: form.byWeekday.includes(day)
        ? form.byWeekday.filter((d) => d !== day)
        : [...form.byWeekday, day],
    });

  return (
    <div className="flex flex-col gap-3.5">
      {/* 1. 제목 */}
      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor={id("title")}>
          제목
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

      {/* 2. 한번만 / 반복 */}
      <div className="flex flex-col gap-1.5">
        <span className={LABEL}>반복</span>
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => onChange({ repeating: false })}
            aria-pressed={!form.repeating}
            className={`${TOGGLE} ${form.repeating ? TOGGLE_OFF : TOGGLE_ON}`}
          >
            한번만
          </button>
          <button
            type="button"
            onClick={() => onChange({ repeating: true })}
            aria-pressed={form.repeating}
            className={`${TOGGLE} ${form.repeating ? TOGGLE_ON : TOGGLE_OFF}`}
          >
            반복
          </button>
        </div>
      </div>

      {/* 3. 주기 */}
      {form.repeating && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>주기</span>
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

      {/* 4. 카테고리 */}
      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor={id("category")}>
          카테고리 (선택)
        </label>
        {/* 자유 입력. 목록에 없는 이름도 쓸 수 있다 */}
        <input
          id={id("category")}
          ref={ref("category")}
          className={box("category")}
          list={id("category-options")}
          value={form.category}
          onChange={(e) => onChange({ category: e.target.value })}
          placeholder="예: 능력"
          maxLength={50}
          autoComplete="off"
        />
        <datalist id={id("category-options")}>
          {knownCategories.map((c) => (
            <option key={c} value={c} />
          ))}
        </datalist>
      </div>

      {/* 5. 시작 */}
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
            시작시각 (선택)
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

      {/* 6. 종료 — 한 칸이 모드에 따라 다른 곳으로 간다 */}
      <div className="flex flex-wrap gap-3">
        <div className="flex min-w-0 flex-1 basis-40 flex-col gap-1.5">
          <label className={LABEL} htmlFor={id("endDate")}>
            {form.repeating ? "반복 종료일자 (선택)" : "종료일자 (선택)"}
          </label>
          <input
            id={id("endDate")}
            ref={ref("endDate")}
            type="date"
            className={box("endDate")}
            value={form.endDate}
            onChange={(e) => onChange({ endDate: e.target.value })}
          />
          <span className={HINT}>
            {form.repeating
              ? "비우면 계속 반복됩니다."
              : "비우면 시작일자와 같은 날로 봅니다."}
          </span>
        </div>

        <div className="flex min-w-0 flex-1 basis-40 flex-col gap-1.5">
          <label className={LABEL} htmlFor={id("endTime")}>
            종료시각 (선택)
          </label>
          <input
            id={id("endTime")}
            ref={ref("endTime")}
            type="time"
            className={box("endTime")}
            value={form.endTime}
            onChange={(e) => onChange({ endTime: e.target.value })}
          />
          {form.repeating && (
            <span className={HINT}>회차마다 이 시각에 끝납니다.</span>
          )}
        </div>
      </div>

      {/* 7. 매주 반복요일 */}
      {form.repeating && form.freq === "WEEKLY" && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>반복요일</span>
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

      {/* 8. 매월 반복일자 — 백엔드가 받기 전까지 자리만 */}
      {form.repeating && form.freq === "MONTHLY" && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>반복일자</span>
          <p className="rounded-md border border-dashed border-slate-300 px-3 py-2 text-xs text-slate-400">
            매월 반복은 준비 중입니다.
          </p>
        </div>
      )}
    </div>
  );
}
