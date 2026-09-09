import type { ParsedSchedule } from "../types/parse";
import type {
  RecurrenceFreq,
  ScheduleCreateRequest,
  ScheduleDetailResponse,
  Weekday,
} from "../types/schedule";
import { toLocalDate, toLocalDateTime } from "../utils/datetime";

/**
 * 탭마다 쓰는 칸이 달라 검증 절차와 화면이 이 값을 확인함
 */
export type FormVariant = "list" | "calendar" | "habit";

/** 백엔드에 아직 MONTHLY 가 없다. 화면에서 자리만 잡아두고 보내지 않는다 */
export type FormFreq = RecurrenceFreq | "MONTHLY";

export type FormFieldId =
  | "title"
  | "freq"
  | "tags"
  | "startDate"
  | "startTime"
  | "endDate"
  | "endTime"
  | "duration"
  | "byWeekday"
  | "byMonthDay"
  | "place"
  | "twoMinuteAction"
  | "dueOn"
  | "parentId";

/**
 * 입력 폼이 다루는 모델. 날짜와 시각을 따로 둔다.
 *
 * 시각을 비우면 하루 종일이다. 한번만이면 종료일시로 끝을 적고,
 * 반복이면 소요시간으로 적는다 — 회차마다 날짜가 달라 절대 종료시각을 쓸 수 없다.
 */
export interface ScheduleForm {
  title: string;
  repeating: boolean;
  freq: FormFreq;
  tags: string[];
  /** "YYYY-MM-DD" */
  startDate: string;
  /** "HH:mm". 비면 하루 종일 */
  startTime: string;
  endDate: string;
  endTime: string;
  /** 반복일 때만 쓴다. "HH" 와 "mm" 을 따로 받는다 */
  durationHours: string;
  durationMins: string;
  byWeekday: Weekday[];
  /** MONTHLY 용. 백엔드가 받기 전까지 쓰이지 않는다 */
  byMonthDay: number[];
  place: string;
  /** 시작에 필요한 2분 이내의 미니 행동 */
  twoMinuteAction: string;
  /** "YYYY-MM-DD". 언제까지 해내야 하나 */
  dueOn: string;
  /** 상위 일정 id. 빈 문자열이면 최상위 */
  parentId: string;
}

/** 지금 이후의 가장 가까운 정각 */
function nextHourTime(): string {
  const at = new Date();
  at.setHours(at.getHours() + 1, 0, 0, 0);
  return `${String(at.getHours()).padStart(2, "0")}:00`;
}

export function emptyForm(defaultDate?: Date | null): ScheduleForm {
  return {
    title: "",
    repeating: false,
    freq: "WEEKLY",
    tags: [],
    startDate: toLocalDate(defaultDate ?? new Date()),
    startTime: nextHourTime(),
    endDate: "",
    endTime: "",
    durationHours: "",
    durationMins: "",
    byWeekday: [],
    byMonthDay: [],
    place: "",
    twoMinuteAction: "",
    dueOn: "",
    parentId: "",
  };
}

// 받은 일시값(String)을 일자와 시각으로 분리
const splitDateTime = (value: string | undefined): [string, string] =>
  value ? [value.slice(0, 10), value.slice(11, 16)] : ["", ""];

//
export function draftFromParsed(
  parsed: ParsedSchedule,
  fallbackDate: Date | null,
): ScheduleForm {
  const base = emptyForm(fallbackDate);
  const [startDate, startTime] = splitDateTime(parsed.startAt);
  const [endDate, endTime] = splitDateTime(parsed.endAt);

  return {
    ...base,
    title: parsed.title,
    tags: parsed.tags ?? [],
    // 시작일을 비워두지 않는다. 못 채운 칸은 되물음이 따로 알려준다
    startDate: startDate || base.startDate,
    startTime: parsed.allDay ? "" : startTime || base.startTime,
    // 반복이면 종료일자 칸은 반복이 끝나는 날을 뜻한다
    endDate: parsed.recurrence ? (parsed.recurrence.endsOn ?? "") : endDate,
    endTime: parsed.recurrence ? "" : endTime,
    ...(parsed.recurrence
      ? splitDuration(minutesBetween(parsed.startAt, parsed.endAt))
      : { durationHours: "", durationMins: "" }),
    repeating: Boolean(parsed.recurrence),
    freq: parsed.recurrence?.freq ?? base.freq,
    byWeekday: parsed.recurrence?.byWeekday ?? [],
    place: parsed.place ?? "",
  };
}

/**
 * 서버가 준 일정 하나를 폼으로 되돌린다
 * 수정 폼이 지금 값을 띄우는 데 쓴다
 */
export function formFromDetail(detail: ScheduleDetailResponse): ScheduleForm {
  const [startDate, startTime] = splitDateTime(detail.startAt);
  const [endDate, endTime] = splitDateTime(detail.endAt);
  const repeating = Boolean(detail.recurrence);

  return {
    ...emptyForm(),
    title: detail.title,
    repeating,
    freq: detail.recurrence?.freq ?? "WEEKLY",
    tags: detail.tags ?? [],
    // 날짜를 안 정한 항목이면 빈 칸으로 둔다. emptyForm 이 채운 오늘 날짜를 덮는다
    startDate,
    startTime: detail.allDay ? "" : startTime,
    // 반복이면 종료일자 칸은 반복이 끝나는 날을 뜻한다
    endDate: repeating ? (detail.recurrence?.endsOn ?? "") : endDate,
    endTime: repeating ? "" : endTime,
    ...(repeating
      ? splitDuration(minutesBetween(detail.startAt, detail.endAt))
      : { durationHours: "", durationMins: "" }),
    byWeekday: detail.recurrence?.byWeekday ?? [],
    place: detail.place ?? "",
    twoMinuteAction: detail.twoMinuteAction ?? "",
    dueOn: detail.dueOn ?? "",
    parentId: detail.parentId === undefined ? "" : String(detail.parentId),
  };
}

/** 회차 사이의 가장 짧은 간격(시간). 반복 일정의 소요시간 상한이다 */
export function gapHours(freq: FormFreq, byWeekday: Weekday[]): number {
  if (freq === "DAILY") return 24;
  if (byWeekday.length <= 1) return 24 * 7;

  const order: Weekday[] = [
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY",
  ];
  const days = byWeekday.map((d) => order.indexOf(d) + 1).sort((a, b) => a - b);

  let min = 7 - days[days.length - 1] + days[0];
  for (let i = 1; i < days.length; i++) {
    min = Math.min(min, days[i] - days[i - 1]);
  }
  return min * 24;
}

/** 폼의 두 칸을 분으로. 둘 다 비면 소요시간 없음(undefined) */
export function durationMinutesOf(form: ScheduleForm): number | undefined {
  if (!form.durationHours && !form.durationMins) return undefined;
  return Number(form.durationHours || 0) * 60 + Number(form.durationMins || 0);
}

function minutesBetween(startAt?: string, endAt?: string): number | undefined {
  if (!startAt || !endAt) return undefined;

  return Math.round((Date.parse(endAt) - Date.parse(startAt)) / 60000);
}

function splitDuration(total: number | undefined) {
  if (total === undefined || total <= 0)
    return { durationHours: "", durationMins: "" };
  return {
    durationHours: String(Math.floor(total / 60)),
    durationMins: String(total % 60),
  };
}

/**
 * Form 먼저 확인 후 에러 띄우기
 *
 * @param variant 리스트만 날짜 없이 저장할 수 있다
 */
export function formValidationError(
  form: ScheduleForm,
  variant: FormVariant = "calendar",
): string | null {
  if (!form.title.trim()) return "제목을 채워 주세요.";

  // 달력은 날짜가 없으면 회차가 0개라 화면에서 사라진다
  // 반복은 첫 회차를 기준으로 펼치므로 리스트에서도 날짜가 필요하다
  if (!form.startDate && (variant !== "list" || form.repeating)) {
    return "시작일자를 채워 주세요.";
  }

  if (form.repeating) {
    if (form.freq === "MONTHLY") return "매월 반복은 아직 준비 중입니다.";
    if (form.freq === "WEEKLY" && form.byWeekday.length === 0) {
      return "반복할 요일을 하나 이상 골라 주세요.";
    }
    if (form.endDate && form.endDate < form.startDate) {
      return "반복 종료일이 시작일보다 앞설 수 없습니다.";
    }

    const minutes = durationMinutesOf(form);
    if (minutes !== undefined) {
      if (minutes <= 0) return "소요시간을 0보다 크게 적어 주세요.";
      const max = gapHours(form.freq, form.byWeekday) * 60;
      if (minutes > max) {
        return `소요시간은 다음 회차가 시작하기 전에 끝나야 합니다. 최대 ${max / 60}시간.`;
      }
    }
  } else if (form.endDate && form.endDate < form.startDate) {
    return "종료일자가 시작일자보다 앞설 수 없습니다.";
  }

  if (
    !form.repeating &&
    form.startTime &&
    form.endTime &&
    (form.endDate || form.startDate) === form.startDate &&
    form.endTime < form.startTime
  ) {
    return "종료시각이 시작시각보다 앞설 수 없습니다.";
  }

  return null;
}

export function formToCreateRequest(form: ScheduleForm): ScheduleCreateRequest {
  // 날짜를 안 정한 항목은 시작일시 없이 보낸다. 리스트에만 남는다
  const dated = Boolean(form.startDate);
  const allDay = dated && !form.startTime;

  return {
    title: form.title.trim(),
    startAt: dated
      ? `${form.startDate}T${form.startTime || "00:00"}:00`
      : undefined,
    endAt: endAtOf(form),
    allDay,
    tags: form.tags.length > 0 ? form.tags : undefined,
    place: form.place.trim() || undefined,
    twoMinuteAction: form.twoMinuteAction.trim() || undefined,
    dueOn: form.dueOn || undefined,
    parentId: form.parentId ? Number(form.parentId) : undefined,
    recurrence: form.repeating
      ? {
          // MONTHLY 는 검증에서 걸러진다
          freq: form.freq as RecurrenceFreq,
          byWeekday: form.freq === "WEEKLY" ? form.byWeekday : undefined,
          endsOn: form.endDate || undefined,
        }
      : undefined,
  };
}

/**
 * 보낼 종료시각
 * 반복이면 소요시간을 시작에 더해 만듬
 * 백엔드가 이걸 duration_minutes 로 바꿔 저장하므로 자정을 넘어도 된다
 */
function endAtOf(form: ScheduleForm): string | undefined {
  // 시작이 없으면 끝을 잴 기준이 없다. 백엔드도 같은 이유로 거부한다
  if (!form.startDate) return undefined;

  if (form.repeating) {
    const minutes = durationMinutesOf(form);
    if (minutes === undefined) return undefined;
    const end = new Date(`${form.startDate}T${form.startTime || "00:00"}:00`);
    end.setMinutes(end.getMinutes() + minutes);
    return toLocalDateTime(end);
  }

  if (!form.endDate && !form.endTime) return undefined;

  // 종료시각을 안 적었으면 그날 끝까지로 본다
  return `${form.endDate || form.startDate}T${form.endTime || "23:59"}:00`;
}
