import type { RecurrenceResponse, Weekday } from "../types/schedule";

/** 규칙을 사람 말로 적을 때 쓰는 요일 이름 */
const WEEKDAY_LABEL: Record<Weekday, string> = {
  MONDAY: "월",
  TUESDAY: "화",
  WEDNESDAY: "수",
  THURSDAY: "목",
  FRIDAY: "금",
  SATURDAY: "토",
  SUNDAY: "일",
};

/** 자바 DayOfWeek 이름을 요일 번호로. 월요일이 1이다 */
const WEEKDAY_NUMBER: Record<Weekday, number> = {
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
  SUNDAY: 7,
};

const ORDER: Weekday[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

/**
 * 규칙을 한 줄로 적는다
 * 날짜만 보면 왜 그날인지 알 수 없어 규칙을 옆에 둔다
 */
export function describeRecurrence(rule: RecurrenceResponse): string {
  if (rule.freq === "DAILY") return "매일";

  const days = ORDER.filter((d) => rule.byWeekday?.includes(d));
  if (days.length === 0) return "매주";

  return `매주 ${days.map((d) => WEEKDAY_LABEL[d]).join("·")}`;
}

/**
 * 그 날짜에서 규칙의 이웃 회차로 옮긴다
 * 보기만 옮기는 것이라 저장은 건드리지 않는다
 *
 * @param from "YYYY-MM-DD"
 * @param step 1이면 다음, -1이면 이전
 * @returns 규칙 밖으로 나가면 null
 */
export function stepOccurrence(
  rule: RecurrenceResponse,
  from: string,
  step: 1 | -1,
): string | null {
  const days = daysOf(rule);
  if (days.length === 0) return null;

  const cursor = new Date(`${from}T00:00:00`);

  // 한 주를 다 돌아도 못 찾으면 규칙이 그 요일을 하나도 안 갖는다
  for (let i = 0; i < 7; i++) {
    cursor.setDate(cursor.getDate() + step);
    if (days.includes(isoWeekday(cursor))) {
      const next = toKey(cursor);
      return outOfRange(rule, next) ? null : next;
    }
  }
  return null;
}

function daysOf(rule: RecurrenceResponse): number[] {
  if (rule.freq === "DAILY") return [1, 2, 3, 4, 5, 6, 7];
  return (rule.byWeekday ?? []).map((d) => WEEKDAY_NUMBER[d]);
}

/** 일요일을 7로 세는 번호. 자바 DayOfWeek 와 맞춘다 */
function isoWeekday(date: Date): number {
  return date.getDay() === 0 ? 7 : date.getDay();
}

function toKey(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

function outOfRange(rule: RecurrenceResponse, date: string): boolean {
  return rule.endsOn !== undefined && date > rule.endsOn;
}
