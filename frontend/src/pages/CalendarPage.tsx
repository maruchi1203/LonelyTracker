import { useCallback, useEffect, useMemo, useState } from "react";
import { fetchCategories } from "../api/categories";
import {
  changeOccurrenceStatus,
  createSchedule,
  deleteSchedule,
  postponeOccurrence,
} from "../api/schedules";
import CalendarToolbar from "../components/calendar/CalendarToolbar";
import ScheduleCalendar from "../components/layouts/Calendar/ScheduleCalendar";
import QuickAddLauncher from "../components/quickadd/QuickAddLauncher";
import ScheduleList from "../components/ScheduleList";
import { applyFilters, countByCategory } from "../domain/filter";
import { coversDate } from "../domain/occurrence";
import { useCalendarViewState } from "../hooks/useCalendarViewState";
import { useMonthOccurrences } from "../hooks/useMonthOccurrences";
import type {
  UserCategoryResponse,
  DeleteScope,
  ScheduleCreateRequest,
  ScheduleResponse,
} from "../types/schedule";

export default function CalendarPage() {
  const {
    month,
    selectedDate,
    category,
    query,
    setMonth,
    toggleDate,
    setCategory,
    setQuery,
    clearFilters,
  } = useCalendarViewState();
  const { occurrences, loading, error, reload, patchOne, setError } =
    useMonthOccurrences(month);

  const [categories, setCategories] = useState<UserCategoryResponse[]>([]);

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  const loadCategories = useCallback(async () => {
    try {
      setCategories(await fetchCategories());
    } catch {
      // 목록 조회 쪽에서 이미 에러를 보여주므로 여기서는 조용히 넘어간다
    }
  }, []);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  /** 성공 여부를 돌려준다. 실패했는데 폼이 닫히거나 입력이 지워지면 곤란하다. */
  const handleCreate = async (body: ScheduleCreateRequest): Promise<boolean> => {
    setError(null);
    try {
      await createSchedule(body);

      // 저장한 일정이 보고 있는 달 밖이면 그 달로 옮긴다. 저장했는데 아무것도 안 보이면 안 된다
      const created = new Date(body.startAt);
      if (
        created.getFullYear() !== month.getFullYear() ||
        created.getMonth() !== month.getMonth()
      ) {
        setMonth(new Date(created.getFullYear(), created.getMonth(), 1));
      }

      await Promise.all([reload(), loadCategories()]);
      return true;
    } catch (e) {
      fail(e, "일정을 추가하지 못했습니다");
      return false;
    }
  };

  const handleToggleStatus = async (occurrence: ScheduleResponse) => {
    setError(null);
    try {
      patchOne(
        await changeOccurrenceStatus(
          occurrence.id,
          occurrence.occurrenceDate,
          occurrence.status === "DONE" ? "PLANNED" : "DONE",
        ),
      );
    } catch (e) {
      fail(e, "상태를 변경하지 못했습니다");
    }
  };

  const handlePostpone = async (occurrence: ScheduleResponse, to: string) => {
    setError(null);
    try {
      patchOne(
        await postponeOccurrence(occurrence.id, occurrence.occurrenceDate, to),
      );
      // 창 밖으로 미뤘다면 목록에서 사라져야 하므로 다시 받는다
      await reload();
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
      await Promise.all([reload(), loadCategories()]);
    } catch (e) {
      fail(e, "일정을 삭제하지 못했습니다");
    }
  };

  // 칩 개수는 필터를 걸기 전의 창 전체로 센다
  const usage = useMemo(() => countByCategory(occurrences), [occurrences]);

  // 달력에 그릴 것 — 분류와 검색만 반영한다
  const forCalendar = useMemo(
    () => applyFilters(occurrences, { category, query }),
    [occurrences, category, query],
  );

  // 아래 목록에 그릴 것 — 고른 날짜까지 좁힌다.
  // 여러 날에 걸친 일정은 첫날뿐 아니라 걸친 날 모두에서 보여야 한다
  const forList = useMemo(
    () =>
      selectedDate
        ? forCalendar.filter((o) => coversDate(o, selectedDate))
        : forCalendar,
    [forCalendar, selectedDate],
  );

  const doneCount = forList.filter((o) => o.status === "DONE").length;
  const filtering = Boolean(category) || query.trim().length > 0;
  const monthLabel = `${month.getFullYear()}년 ${month.getMonth() + 1}월`;

  return (
    <div className="flex flex-col gap-6">
      <QuickAddLauncher
        // 날짜를 골라둔 상태면 그 날짜로 시작값을 채워준다
        defaultDate={selectedDate}
        knownCategories={categories.map((c) => c.name)}
        onCreate={handleCreate}
      />

      <CalendarToolbar
        query={query}
        onQueryChange={setQuery}
        categories={categories}
        usage={usage}
        total={occurrences.length}
        monthLabel={monthLabel}
        selectedCategory={category}
        onSelectCategory={setCategory}
      />

      <ScheduleCalendar
        month={month}
        onMonthChange={setMonth}
        selectedDate={selectedDate}
        onSelectDate={toggleDate}
        occurrences={forCalendar}
        loading={loading}
      />

      <section className="flex flex-col gap-2">
        <div className="flex items-baseline gap-2">
          <h2 className="text-lg font-semibold text-slate-800">
            {selectedDate
              ? `${selectedDate.getMonth() + 1}월 ${selectedDate.getDate()}일`
              : "이 달 전체"}
          </h2>
          <p className="text-sm text-slate-500">
            {forList.length}건 · 완료 {doneCount}건
          </p>
        </div>

        {error && (
          <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-600">
            {error}
          </p>
        )}

        <ScheduleList
          occurrences={forList}
          onToggleStatus={handleToggleStatus}
          onPostpone={handlePostpone}
          onDelete={handleDelete}
          emptyReason={filtering ? "filtered-out" : "no-data"}
          onClearFilters={clearFilters}
        />
      </section>
    </div>
  );
}
