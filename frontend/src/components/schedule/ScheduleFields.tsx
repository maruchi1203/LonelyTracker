import { useState } from "react";
import {
  durationMinutesOf,
  gapHours,
  type FormFieldId,
  type FormFreq,
  type ScheduleForm,
} from "../../domain/scheduleForm";
import type { Weekday } from "../../types/schedule";

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
  /** 2분 행동 칸을 띄울지. 설정에서 꺼도 이미 적어둔 값이 있으면 보인다 */
  showTwoMinute?: boolean;
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
  knownTags,
  idPrefix,
  fieldRef,
  decorate,
  showTwoMinute = true,
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

  const maxHours = gapHours(form.freq, form.byWeekday);

  /** 소요시간을 적으면 실제로 몇 시에 끝나는지 보여준다 */
  const endsAtHint = (() => {
    const minutes = durationMinutesOf(form);
    if (!form.repeating || minutes === undefined || minutes <= 0) return null;

    const end = new Date(`${form.startDate}T${form.startTime || "00:00"}:00`);
    if (Number.isNaN(end.getTime())) return null;
    end.setMinutes(end.getMinutes() + minutes);

    const time = `${String(end.getHours()).padStart(2, "0")}:${String(
      end.getMinutes(),
    ).padStart(2, "0")}`;
    const days = Math.floor(
      (end.getTime() - new Date(`${form.startDate}T00:00:00`).getTime()) / 86400000,
    );
    return days > 0 ? `→ ${days}일 뒤 ${time} 에 끝납니다.` : `→ ${time} 에 끝납니다.`;
  })();

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

      {/* 4. 태그 */}
      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor={id("tags")}>
          태그 (선택)
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

      {/* 4-1. 기한 */}
      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor={id("dueOn")}>
          기한 (선택)
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

      {/* 5. 장소 */}
      <div className="flex flex-col gap-1.5">
        <label className={LABEL} htmlFor={id("place")}>
          장소 (선택)
        </label>
        <input
          id={id("place")}
          ref={ref("place")}
          className={box("place")}
          value={form.place}
          onChange={(e) => onChange({ place: e.target.value })}
          placeholder="예: 헬스장"
          maxLength={200}
        />
        <span className={HINT}>언제·어디서가 구체적일수록 실행될 확률이 높습니다.</span>
      </div>

      {/* 6. 시작 */}
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

      {/* 7. 끝 — 한번만은 종료일시, 반복은 소요시간 + 반복 종료일 */}
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

        {form.repeating ? (
          <div className="flex min-w-0 flex-1 basis-40 flex-col gap-1.5">
            <label className={LABEL} htmlFor={id("durationHours")}>
              소요시간 (선택)
            </label>
            <div className="flex items-center gap-1.5" ref={ref("duration")}>
              <input
                id={id("durationHours")}
                type="number"
                min={0}
                max={Math.floor(maxHours)}
                className={`${box("duration")} w-16`}
                value={form.durationHours}
                onChange={(e) => onChange({ durationHours: e.target.value })}
              />
              <span className="text-sm text-slate-500">시간</span>
              <input
                id={id("durationMins")}
                type="number"
                min={0}
                max={59}
                step={5}
                className={`${box("duration")} w-16`}
                value={form.durationMins}
                onChange={(e) => onChange({ durationMins: e.target.value })}
              />
              <span className="text-sm text-slate-500">분</span>
            </div>
            <span className={HINT}>
              {endsAtHint ?? `다음 회차 전까지, 최대 ${maxHours}시간.`}
            </span>
          </div>
        ) : (
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
          </div>
        )}
      </div>

      {/* 8. 매주 반복요일 */}
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

      {/* 9. 매월 반복일자 — 백엔드가 받기 전까지 자리만 */}
      {form.repeating && form.freq === "MONTHLY" && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>반복일자</span>
          <p className="rounded-md border border-dashed border-slate-300 px-3 py-2 text-xs text-slate-400">
            매월 반복은 준비 중입니다.
          </p>
        </div>
      )}

      {/* 10. 2분 행동 */}
      {(showTwoMinute || form.twoMinuteAction) && (
        <div className="flex flex-col gap-1.5">
          <label className={LABEL} htmlFor={id("twoMinuteAction")}>
            2분 행동 (선택)
          </label>
          <input
            id={id("twoMinuteAction")}
            ref={ref("twoMinuteAction")}
            className={box("twoMinuteAction")}
            value={form.twoMinuteAction}
            onChange={(e) => onChange({ twoMinuteAction: e.target.value })}
            placeholder="예: 퇴근 후 운동복 갈아입기"
            maxLength={200}
          />
          <span className={HINT}>시작에 필요한 2분 이내의 행동을 적습니다.</span>
        </div>
      )}
    </div>
  );
}
