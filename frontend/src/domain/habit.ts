import type { Habit, HabitCategory } from "../types/habit";

export interface CategoryGroup {
  category: HabitCategory;
  habits: Habit[];
}

/**
 * 카테고리마다 묶는다
 *
 * 카테고리 목록을 받아서 도는 까닭은, 습관에서 카테고리를 모으면 비어 있는 카테고리가 사라져서다.
 * 빈 카테고리도 자리를 지켜야 거기에 습관을 넣는 단추가 걸린다.
 * 차례는 목록이 온 순서를 그대로 쓴다 — 서버가 display_order 로 정렬해 준다
 */
export function groupByCategory(
  habits: Habit[],
  categories: HabitCategory[],
): CategoryGroup[] {
  return categories.map((category) => ({
    category,
    habits: habits.filter((h) => h.categoryId === category.id),
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
