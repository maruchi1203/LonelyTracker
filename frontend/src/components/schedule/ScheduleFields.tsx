import { useState, type ReactNode } from "react";
import IconButton from "../layouts/IconButton";
import {
  ClockIcon,
  CloseIcon,
  CouldIcon,
  DailyIcon,
  MomentIcon,
  MonthlyIcon,
  MustIcon,
  RangeIcon,
  RepeatIcon,
  ShouldIcon,
  WeeklyIcon,
  WontIcon,
} from "../layouts/Icons";
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
  "w-full rounded-md border bg-surface px-2.5 py-2 text-ink placeholder:text-ink-faint transition-colors focus:border-accent focus:outline-none focus:ring-3 focus:ring-line";
const LABEL = "text-xs font-semibold tracking-wide text-ink-soft";

/** 그림 하나를 그리는 함수. 표에 담아 두고 자리에서 불러 쓴다 */
type Icon = () => ReactNode;

/** 폼을 가르는 종류. 고른 것에 따라 아래 칸이 바뀐다 */
const KINDS: { value: FormKind; label: string; hint: string; Icon: Icon }[] = [
  {
    value: "simple",
    label: "간단",
    hint: "언제 할지만 적습니다",
    Icon: MomentIcon,
  },
  {
    value: "period",
    label: "기간",
    hint: "시작과 끝이 있는 일입니다",
    Icon: RangeIcon,
  },
  {
    value: "repeat",
    label: "반복",
    hint: "되풀이하는 일입니다",
    Icon: RepeatIcon,
  },
];

const FREQ: { value: FormFreq; label: string; ready: boolean; Icon: Icon }[] = [
  { value: "DAILY", label: "매일", ready: true, Icon: DailyIcon },
  { value: "WEEKLY", label: "매주", ready: true, Icon: WeeklyIcon },
  { value: "MONTHLY", label: "매월", ready: false, Icon: MonthlyIcon },
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
const PRIORITIES: {
  value: SchedulePriority;
  label: string;
  hint: string;
  Icon: Icon;
}[] = [
  { value: "MUST", label: "필수", hint: "반드시 해야 합니다", Icon: MustIcon },
  {
    value: "SHOULD",
    label: "권장",
    hint: "하는 편이 좋습니다",
    Icon: ShouldIcon,
  },
  { value: "COULD", label: "선택", hint: "여유가 되면 합니다", Icon: CouldIcon },
  {
    value: "WONT",
    label: "보류",
    hint: "안 하기로 했습니다. 달력에서 빠집니다",
    Icon: WontIcon,
  },
];

/** 한 줄을 고르게 나눠 갖는다. 고를 것이 몇 개든 줄이 넘치지 않는다 */
const ROW = "flex items-stretch gap-2";
const TOGGLE_ON = "border-accent bg-accent text-canvas";
const TOGGLE_OFF = "border-line text-ink-soft hover:bg-accent-soft";

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
    `${INPUT} ${decorate?.(name) ?? "border-line"}`;

  const [tagDraft, setTagDraft] = useState("");

  /**
   * 시각 칸을 펼쳐 두라고 손으로 이른 것.
   * 적어 둔 값이 있으면 이르지 않아도 펼친다 — AI 초안이나 수정 폼이 그렇다
   */
  const [asked, setAsked] = useState({ start: false, end: false });

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
        <div className={ROW}>
          {KINDS.map(({ value, label, hint, Icon }) => (
            <IconButton
              key={value}
              wide
              label={label}
              title={hint}
              pressed={form.kind === value}
              onClick={() => onChange({ kind: value })}
            >
              <Icon />
            </IconButton>
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
        <div ref={ref("priority")} className={ROW}>
          {PRIORITIES.map(({ value, label, hint, Icon }) => (
            <IconButton
              key={value}
              wide
              label={label}
              title={hint}
              // 안 고른 상태에서도 "선택"이 눌린 것처럼 보인다. 값은 여전히 비어 있다
              pressed={(form.priority || "COULD") === value}
              onClick={() => onChange({ priority: value })}
            >
              <Icon />
            </IconButton>
          ))}
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
                className="rounded-full border border-line bg-accent-soft px-2.5 py-1 text-xs text-accent hover:bg-accent-soft"
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
      <div className="flex flex-col gap-3">
        <DateRow
          label="시작일자"
          id={id("startDate")}
          inputRef={ref("startDate")}
          className={box("startDate")}
          value={form.startDate}
          onChange={(startDate) => onChange({ startDate })}
          timeShown={asked.start || Boolean(form.startTime)}
          onAddTime={() => setAsked((a) => ({ ...a, start: true }))}
        />

        {(asked.start || form.startTime) && (
          <TimeRow
            label="시작시각"
            id={id("startTime")}
            inputRef={ref("startTime")}
            className={box("startTime")}
            value={form.startTime}
            onChange={(startTime) => onChange({ startTime })}
            onDrop={() => {
              onChange({ startTime: "" });
              setAsked((a) => ({ ...a, start: false }));
            }}
          />
        )}
      </div>

      {/* 6. 기간 — 끝 */}
      {form.kind === "period" && (
        <div className="flex flex-col gap-3">
          <DateRow
            label="종료일자"
            id={id("endDate")}
            inputRef={ref("endDate")}
            className={box("endDate")}
            value={form.endDate}
            onChange={(endDate) => onChange({ endDate })}
            timeShown={asked.end || Boolean(form.endTime)}
            onAddTime={() => setAsked((a) => ({ ...a, end: true }))}
          />

          {(asked.end || form.endTime) && (
            <TimeRow
              label="종료시각"
              id={id("endTime")}
              inputRef={ref("endTime")}
              className={box("endTime")}
              value={form.endTime}
              onChange={(endTime) => onChange({ endTime })}
              onDrop={() => {
                onChange({ endTime: "" });
                setAsked((a) => ({ ...a, end: false }));
              }}
            />
          )}
        </div>
      )}

      {/* 7. 반복 — 주기 */}
      {form.kind === "repeat" && (
        <div className="flex flex-col gap-1.5">
          <span className={LABEL}>주기 *</span>
          <div ref={ref("freq")} className={ROW}>
            {FREQ.map(({ value: freq, label, ready, Icon }) => (
              <IconButton
                key={freq}
                wide
                label={label}
                disabled={!ready}
                title={ready ? undefined : "준비 중입니다"}
                pressed={form.freq === freq}
                onClick={() => onChange({ freq })}
              >
                <Icon />
              </IconButton>
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
            className={`${ROW} rounded-md ${decorate?.("byWeekday") ?? ""}`}
          >
            {WEEKDAYS.map(({ value: day, label }) => (
              <button
                key={day}
                type="button"
                onClick={() => toggleWeekday(day)}
                aria-pressed={form.byWeekday.includes(day)}
                className={`h-9 flex-1 rounded-full border text-sm transition-colors ${
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
          <p className="rounded-md border border-dashed border-line px-3 py-2 text-xs text-ink-faint">
            매월 반복은 준비 중입니다.
          </p>
        </div>
      )}
    </div>
  );
}

/** 칸 하나와 그 옆에 붙는 동그란 단추. 일자와 시각 줄이 같은 모양을 쓴다 */
function FieldRow({
  label,
  id,
  children,
  button,
}: {
  label: string;
  id: string;
  children: ReactNode;
  button: ReactNode;
}) {
  return (
    <div className="flex items-end gap-2">
      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        <label className={LABEL} htmlFor={id}>
          {label}
        </label>
        {children}
      </div>
      {button}
    </div>
  );
}

interface RowProps {
  label: string;
  id: string;
  inputRef: ((el: HTMLElement | null) => void) | undefined;
  className: string;
  value: string;
  onChange: (value: string) => void;
}

/** 일자 한 줄. 시각을 아직 안 적었으면 옆 단추로 불러낸다 */
function DateRow({
  timeShown,
  onAddTime,
  ...field
}: RowProps & { timeShown: boolean; onAddTime: () => void }) {
  return (
    <FieldRow
      label={field.label}
      id={field.id}
      button={
        timeShown ? null : (
          <IconButton
            label="시각 넣기"
            title="시각을 안 정하면 하루 종일로 저장됩니다"
            onClick={onAddTime}
          >
            <ClockIcon />
          </IconButton>
        )
      }
    >
      <input
        id={field.id}
        ref={field.inputRef}
        type="date"
        className={field.className}
        value={field.value}
        onChange={(e) => field.onChange(e.target.value)}
      />
    </FieldRow>
  );
}

/** 시각 한 줄. 옆 단추로 다시 접으면 값도 함께 지운다 */
function TimeRow({ onDrop, ...field }: RowProps & { onDrop: () => void }) {
  return (
    <FieldRow
      label={field.label}
      id={field.id}
      button={
        <IconButton label="시각 빼기" onClick={onDrop}>
          <CloseIcon />
        </IconButton>
      }
    >
      <input
        id={field.id}
        ref={field.inputRef}
        type="time"
        className={field.className}
        value={field.value}
        onChange={(e) => field.onChange(e.target.value)}
      />
    </FieldRow>
  );
}
