import { useCallback, useEffect, useState } from "react";
import { fetchCategories } from "../api/categories";
import {
  changeOccurrenceStatus,
  createSchedule,
  deleteSchedule,
  fetchSchedules,
  postponeOccurrence,
} from "../api/schedules";
import CategoryChips from "../components/calendar/CategoryChips";
import ScheduleCalendar from "../components/layouts/Calendar/ScheduleCalendar";
import ScheduleInputForm from "../components/ScheduleInputForm";
import ScheduleList from "../components/ScheduleList";
import { replaceOccurrence } from "../domain/occurrence";
import type {
  CategoryResponse,
  DeleteScope,
  ScheduleCreateRequest,
  ScheduleResponse,
} from "../types/schedule";
import { isSameDay } from "../utils/datetime";

export default function CalendarPage() {
  const [occurrences, setOccurrences] = useState<ScheduleResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  const loadCategories = useCallback(async () => {
    try {
      setCategories(await fetchCategories());
    } catch {
      // 목록 조회 쪽에서 이미 에러를 보여주므로 여기서는 조용히 넘어간다
    }
  }, []);

  const load = useCallback(async (category: string | null) => {
    setLoading(true);
    setError(null);
    try {
      setOccurrences(await fetchSchedules(category ? { category } : undefined));
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
      await Promise.all([load(selectedCategory), loadCategories()]);
      setFormOpen(false);
      return true;
    } catch (e) {
      fail(e, "일정을 추가하지 못했습니다");
      return false;
    }
  };

  const handleToggleStatus = async (occurrence: ScheduleResponse) => {
    setError(null);
    try {
      const updated = await changeOccurrenceStatus(
        occurrence.id,
        occurrence.occurrenceDate,
        occurrence.status === "DONE" ? "PLANNED" : "DONE",
      );
      setOccurrences((prev) => replaceOccurrence(prev, updated));
    } catch (e) {
      fail(e, "상태를 변경하지 못했습니다");
    }
  };

  const handlePostpone = async (occurrence: ScheduleResponse, to: string) => {
    setError(null);
    try {
      const updated = await postponeOccurrence(
        occurrence.id,
        occurrence.occurrenceDate,
        to,
      );
      setOccurrences((prev) => replaceOccurrence(prev, updated));
    } catch (e) {
      fail(e, "일정을 미루지 못했습니다");
    }
  };

  /** FUTURE 는 지난 회차를 남기므로 화면에서 거르지 말고 다시 받아야 한다 */
  const handleDelete = async (
    occurrence: ScheduleResponse,
    scope: DeleteScope,
  ) => {
    setError(null);
    try {
      await deleteSchedule(occurrence.id, scope);
      await Promise.all([load(selectedCategory), loadCategories()]);
    } catch (e) {
      fail(e, "일정을 삭제하지 못했습니다");
    }
  };

  // 달력은 전체를 받고, 목록만 선택한 날짜로 좁힌다
  const visibleOccurrences = selectedDate
    ? occurrences.filter((o) => isSameDay(new Date(o.startAt), selectedDate))
    : occurrences;

  const doneCount = visibleOccurrences.filter((o) => o.status === "DONE").length;

  return (
    <div className="flex flex-col gap-6">
      <CategoryChips
        categories={categories}
        selected={selectedCategory}
        onSelect={setSelectedCategory}
      />

      <ScheduleCalendar
        occurrences={occurrences}
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
              {visibleOccurrences.length}건 · 완료 {doneCount}건
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
            occurrences={visibleOccurrences}
            onToggleStatus={handleToggleStatus}
            onPostpone={handlePostpone}
            onDelete={handleDelete}
          />
        )}
      </section>
    </div>
  );
}
