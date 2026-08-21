import { useCallback, useEffect, useState } from "react";
import { fetchCategories } from "./api/categories";
import {
  changeStatus,
  createSchedule,
  deleteSchedule,
  fetchSchedules,
} from "./api/schedules";
import AppShell from "./components/layouts/AppShell";
import ScheduleCalendar from "./components/layouts/Calendar/ScheduleCalendar";
import ScheduleInputForm from "./components/ScheduleInputForm";
import ScheduleList from "./components/ScheduleList";
import type {
  Category,
  Schedule,
  ScheduleCreateRequest,
} from "./types/schedule";

/** 두 시각이 같은 날인지 (시·분은 무시) */
function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export default function App() {
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  // 카테고리는 이제 독립 리소스라 일정 목록과 무관하게 가져옴
  const loadCategories = useCallback(async () => {
    try {
      setCategories(await fetchCategories());
    } catch {
      // 목록 조회 쪽에서 이미 에러를 보여주므로 여기서는 조용히 넘어간다
    }
  }, []);

  /**
   * 서버 조회는 분류까지만 좁힌다.
   * 날짜로도 좁히면 달력이 그 하루치만 받게 되어 나머지 칸이 비어 보인다.
   * 선택한 날짜는 아래 visibleSchedules 에서 화면단으로 거른다.
   */
  const load = useCallback(async (category: string | null) => {
    setLoading(true);
    setError(null);
    try {
      setSchedules(await fetchSchedules(category ? { category } : undefined));
    } catch (e) {
      fail(e, "목록을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load(selectedCategory);
  }, [load, selectedCategory]);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  /** 성공 여부를 돌려준다. 실패했는데 폼이 닫히거나 입력이 지워지면 곤란하다. */
  const handleCreate = async (body: ScheduleCreateRequest): Promise<boolean> => {
    setError(null);
    try {
      await createSchedule(body);
      // 새 일정이 현재 필터에 맞는지는 서버가 판단하므로 다시 받아온다
      await Promise.all([load(selectedCategory), loadCategories()]);
      setFormOpen(false);
      return true;
    } catch (e) {
      fail(e, "일정을 추가하지 못했습니다");
      return false;
    }
  };

  const handleToggleStatus = async (schedule: Schedule) => {
    setError(null);
    try {
      const updated = await changeStatus(
        schedule.id,
        schedule.status === "DONE" ? "PLANNED" : "DONE",
      );
      setSchedules((prev) =>
        prev.map((s) => (s.id === updated.id ? updated : s)),
      );
    } catch (e) {
      fail(e, "상태를 변경하지 못했습니다");
    }
  };

  const handleDelete = async (id: number) => {
    setError(null);
    try {
      await deleteSchedule(id);
      setSchedules((prev) => prev.filter((s) => s.id !== id));
      await loadCategories();
    } catch (e) {
      fail(e, "일정을 삭제하지 못했습니다");
    }
  };

  // 달력은 전체를 받고, 목록만 선택한 날짜로 좁힌다
  const visibleSchedules = selectedDate
    ? schedules.filter((s) => isSameDay(new Date(s.startAt), selectedDate))
    : schedules;

  const doneCount = visibleSchedules.filter((s) => s.status === "DONE").length;

  return (
    <AppShell
      categories={categories}
      selectedCategory={selectedCategory}
      onSelectCategory={setSelectedCategory}
    >
      <div className="flex flex-col gap-6">
        <ScheduleCalendar
          schedules={schedules}
          // 같은 날짜를 다시 누르면 선택을 푼다
          onSelectDate={(date) =>
            setSelectedDate((prev) =>
              prev && isSameDay(prev, date) ? null : date,
            )
          }
        />

        <section className="flex flex-col gap-2">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-baseline gap-2">
              <h2 className="text-lg font-semibold text-slate-800">
                {selectedDate
                  ? `${selectedDate.getMonth() + 1}월 ${selectedDate.getDate()}일`
                  : "전체 일정"}
              </h2>
              <p className="text-sm text-slate-500">
                {visibleSchedules.length}건 · 완료 {doneCount}건
              </p>
            </div>

            <button
              type="button"
              onClick={() => setFormOpen((open) => !open)}
              aria-expanded={formOpen}
              aria-label={formOpen ? "일정 추가 닫기" : "일정 추가 열기"}
              className={`flex size-8 shrink-0 items-center justify-center rounded-full text-xl leading-none transition-all focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100 ${
                formOpen
                  ? "rotate-45 bg-slate-200 text-slate-600"
                  : "bg-brand-500 text-white hover:bg-brand-600"
              }`}
            >
              +
            </button>
          </div>

          {formOpen && (
            <ScheduleInputForm
              onSubmit={handleCreate}
              knownCategories={categories.map((c) => c.name)}
              // 날짜를 골라둔 상태면 그 날짜로 시작값을 채워준다
              defaultDate={selectedDate}
              disabled={loading}
            />
          )}

          {error && (
            <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-600">
              {error}
            </p>
          )}

          {loading ? (
            <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-12 text-center text-sm text-slate-400">
              불러오는 중…
            </p>
          ) : (
            <ScheduleList
              schedules={visibleSchedules}
              onToggleStatus={handleToggleStatus}
              onDelete={handleDelete}
            />
          )}
        </section>
      </div>
    </AppShell>
  );
}
