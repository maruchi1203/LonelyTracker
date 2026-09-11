import type { Habit, HabitCategory } from "../types/habit";

/** 화면에 적는 갈래 이름. 차례도 이 순서를 따른다 */
export const HABIT_CATEGORIES: { value: HabitCategory; label: string }[] = [
  { value: "BODY", label: "운동" },
  { value: "MIND", label: "마음챙김" },
  { value: "SIDE_JOB", label: "부업" },
  { value: "ART", label: "예술" },
  { value: "LEARNING", label: "학습" },
  { value: "RELATIONSHIP", label: "인간관계" },
];

export interface CategoryGroup {
  category: HabitCategory;
  label: string;
  habits: Habit[];
}

/**
 * 갈래마다 묶는다
 * 비어 있는 갈래도 자리를 남긴다. 여섯 갈래를 채우기를 권하는 화면이라 빈 칸이 보여야 한다
 */
export function groupByCategory(habits: Habit[]): CategoryGroup[] {
  return HABIT_CATEGORIES.map(({ value, label }) => ({
    category: value,
    label,
    habits: habits.filter((h) => h.category === value),
  }));
}

/**
 * 오늘까지 이어 온 날 수
 * 어제까지만 해 두고 오늘을 아직 안 했어도 끊긴 것으로 보지 않는다
 *
 * @param today "YYYY-MM-DD"
 */
export function streakOf(habit: Habit, today: string): number {
  const done = new Set(habit.doneDates);

  // 오늘을 아직 안 했으면 어제부터 센다
  const cursor = new Date(`${today}T00:00:00`);
  if (!done.has(today)) cursor.setDate(cursor.getDate() - 1);

  let days = 0;
  while (done.has(keyOf(cursor))) {
    days++;
    cursor.setDate(cursor.getDate() - 1);
  }
  return days;
}

/** 최근 며칠의 날짜. 오늘이 맨 뒤다 */
export function recentDays(today: string, count: number): string[] {
  const days: string[] = [];
  const cursor = new Date(`${today}T00:00:00`);
  cursor.setDate(cursor.getDate() - (count - 1));

  for (let i = 0; i < count; i++) {
    days.push(keyOf(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return days;
}

function keyOf(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}
