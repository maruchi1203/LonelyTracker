import type { RecurrenceDraft } from "../domain/draft";
import type { RecurrenceFreq, Weekday } from "../types/schedule";

interface Props {
  value: RecurrenceDraft;
  onChange: (patch: Partial<RecurrenceDraft>) => void;
  /** 입력칸이 여럿 뜰 수 있어 id 가 겹치지 않게 한다 */
  idPrefix?: string;
  weekdayRef?: (el: HTMLElement | null) => void;
  endsOnRef?: (el: HTMLInputElement | null) => void;
  /** 되물음이 가리키는 칸을 표시한다. 수동 입력에는 되물음이 없다 */
  highlight?: { byWeekday: boolean; endsOn: boolean };
}

const WEEKDAYS: { value: Weekday; label: string }[] = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" },
];

const FREQ: { value: RecurrenceFreq; label: string }[] = [
  { value: "DAILY", label: "매일" },
  { value: "WEEKLY", label: "매주" },
];

const LABEL = "text-xs font-semibold tracking-wide text-slate-500";

export default function RecurrenceEditor({
  value: draft,
  onChange,
  idPrefix = "draft",
  weekdayRef,
  endsOnRef,
  highlight = { byWeekday: false, endsOn: false },
}: Props) {
  const weeklyDisabled = draft.freq === "DAILY";
  const endsOnId = `${idPrefix}-endsOn`;

  const toggleWeekday = (value: Weekday) => {
    const next = draft.byWeekday.includes(value)
      ? draft.byWeekday.filter((d) => d !== value)
      : [...draft.byWeekday, value];
    onChange({ byWeekday: next });
  };

  return (
    <div className="flex flex-col gap-3 rounded-xl border border-slate-200 p-3">
      <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 select-none">
        <input
          type="checkbox"
          className="size-4 cursor-pointer accent-brand-500"
          checked={draft.recurring}
          onChange={(e) => onChange({ recurring: e.target.checked })}
        />
        반복 일정
      </label>

      {draft.recurring && (
        <>
          <div className="flex items-center gap-1.5">
            {FREQ.map(({ value, label }) => (
              <button
                key={value}
                type="button"
                onClick={() => onChange({ freq: value })}
                aria-pressed={draft.freq === value}
                className={`rounded-md border px-3 py-1 text-sm transition-colors ${
                  draft.freq === value
                    ? "border-brand-500 bg-brand-500 text-white"
                    : "border-slate-200 text-slate-600 hover:bg-brand-50"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          <div
            ref={weekdayRef}
            tabIndex={-1}
            className={`flex flex-col gap-1.5 rounded-md ${
              highlight.byWeekday ? "ring-2 ring-amber-300" : ""
            }`}
          >
            <span className={LABEL}>요일</span>
            <div className="flex flex-wrap gap-1">
              {WEEKDAYS.map(({ value, label }) => (
                <button
                  key={value}
                  type="button"
                  disabled={weeklyDisabled}
                  onClick={() => toggleWeekday(value)}
                  aria-pressed={draft.byWeekday.includes(value)}
                  className={`size-8 rounded-full border text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
                    draft.byWeekday.includes(value)
                      ? "border-brand-500 bg-brand-500 text-white"
                      : "border-slate-200 text-slate-600 hover:bg-brand-50"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            {weeklyDisabled && (
              <span className="text-xs text-slate-400">
                매일 반복이라 요일은 쓰이지 않습니다.
              </span>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label className={LABEL} htmlFor={endsOnId}>
              종료일 (비우면 계속)
            </label>
            <input
              id={endsOnId}
              ref={endsOnRef}
              type="date"
              value={draft.endsOn}
              onChange={(e) => onChange({ endsOn: e.target.value })}
              className={`w-44 rounded-md border px-2.5 py-2 text-slate-800 focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100 ${
                highlight.endsOn ? "border-amber-300" : "border-slate-200"
              }`}
            />
          </div>
        </>
      )}
    </div>
  );
}
