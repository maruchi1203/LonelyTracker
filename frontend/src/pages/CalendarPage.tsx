import { useCallback, useEffect, useMemo, useState } from "react";
import {
  changeCompletion,
  changeInstanceStatus,
  createSchedule,
  deleteSchedule,
  fetchTagNames,
  updateInstance,
} from "../api/schedules";
import CalendarToolbar from "../components/layouts/Calendar/CalendarToolbar";
import ScheduleCalendar from "../components/layouts/Calendar/ScheduleCalendar";
import { useQuickAddTarget } from "../components/quickadd/QuickAddContext";
import InstanceActionModal from "../components/layouts/Calendar/InstanceActionModal";
import { applyFilters, countByTag } from "../domain/filter";
import { useCalendarViewState } from "../hooks/useCalendarViewState";
import { useMonthInstances } from "../hooks/useMonthInstances";
import type {
  DeleteScope,
  ScheduleCreateRequest,
  ScheduleResponse,
} from "../types/schedule";

export default function CalendarPage() {
  const {
    month,
    selectedDate,
    tag,
    query,
    setMonth,
    toggleDate,
    setTag,
    setQuery,
    clearFilters,
  } = useCalendarViewState();
  const { instances, loading, error, reload, patchOne, setError } =
    useMonthInstances(month);

  const [knownTags, setKnownTags] = useState<string[]>([]);

  /** 칸에서 고른 회차. 무엇을 할지는 가운데 창이 묻는다 */
  const [picked, setPicked] = useState<ScheduleResponse | null>(null);

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  const loadTags = useCallback(async () => {
    try {
      setKnownTags(await fetchTagNames());
    } catch {
      // 목록 조회 쪽에서 이미 에러를 보여주므로 여기서는 조용히 넘어간다
    }
  }, []);

  useEffect(() => {
    void loadTags();
  }, [loadTags]);

  /** 성공 여부를 돌려준다. 실패했는데 폼이 닫히거나 입력이 지워지면 곤란하다. */
  const handleCreate = async (body: ScheduleCreateRequest): Promise<boolean> => {
    setError(null);
    try {
      await createSchedule(body);

      // 저장한 일정이 보고 있는 달 밖이면 그 달로 옮긴다. 저장했는데 아무것도 안 보이면 안 된다
      if (body.startAt) {
        const created = new Date(body.startAt);
        if (
          created.getFullYear() !== month.getFullYear() ||
          created.getMonth() !== month.getMonth()
        ) {
          setMonth(new Date(created.getFullYear(), created.getMonth(), 1));
        }
      }

      await Promise.all([reload(), loadTags()]);
      return true;
    } catch (e) {
      fail(e, "일정을 추가하지 못했습니다");
      return false;
    }
  };

  const handleToggleStatus = async (instance: ScheduleResponse) => {
    setError(null);
    const done = instance.status === "DONE";
    try {
      // 1회성의 완료는 일정 자체가, 습관의 완료는 회차가 갖는다
      patchOne(
        instance.recurring && instance.instanceDate
          ? await changeInstanceStatus(
              instance.id,
              instance.instanceDate,
              done ? "PLANNED" : "DONE",
            )
          : await changeCompletion(instance.id, !done),
      );
    } catch (e) {
      fail(e, "상태를 변경하지 못했습니다");
    }
  };

  const handleMove = async (instance: ScheduleResponse, startAt: string) => {
    setError(null);
    try {
      if (!instance.instanceDate) return;
      // 종료를 안 보내면 일정의 소요시간을 그대로 쓴다
      patchOne(
        await updateInstance(instance.id, instance.instanceDate, { startAt }),
      );
      // 창 밖으로 옮겼다면 목록에서 사라져야 하므로 다시 받는다
      await reload();
    } catch (e) {
      fail(e, "일정을 옮기지 못했습니다");
    }
  };

  /** 건너뛰기는 습관에만 있다. 지키기로 한 규칙이 있어야 안 지킨 것도 성립한다 */
  const handleSkip = async (instance: ScheduleResponse) => {
    setError(null);
    if (!instance.recurring || !instance.instanceDate) return;
    try {
      patchOne(
        await changeInstanceStatus(instance.id, instance.instanceDate, "SKIPPED"),
      );
    } catch (e) {
      fail(e, "건너뛰지 못했습니다");
    }
  };

  /** FUTURE 는 지난 회차를 남기므로 화면에서 거르지 말고 다시 받아야 한다 */
  const handleDelete = async (
    instance: ScheduleResponse,
    scope: DeleteScope,
  ) => {
    setError(null);
    try {
      await deleteSchedule(instance.id, scope);
      await Promise.all([reload(), loadTags()]);
    } catch (e) {
      fail(e, "일정을 삭제하지 못했습니다");
    }
  };

  // 칩 개수는 필터를 걸기 전의 창 전체로 센다
  const usage = useMemo(() => countByTag(instances), [instances]);

  // 달력에 그릴 것 — 분류와 검색만 반영한다. 격자에는 회차가 전부 선다
  const forCalendar = useMemo(
    () => applyFilters(instances, { tag, query }),
    [instances, tag, query],
  );

  const monthLabel = `${month.getFullYear()}년 ${month.getMonth() + 1}월`;

  // 걸러낸 것이 있으면 알려야 한다. 아무것도 없는 달력과 구별되지 않는다
  const filtering = Boolean(tag) || query.trim().length > 0;

  // 날짜를 골라 뒀으면 빠른 추가의 시작값이 된다
  useQuickAddTarget(
    { defaultDate: selectedDate, knownTags, variant: "calendar" },
    handleCreate,
  );

  return (
    <div className="flex flex-col gap-6">

      <CalendarToolbar
        query={query}
        onQueryChange={setQuery}
        known={knownTags}
        usage={usage}
        total={instances.length}
        monthLabel={monthLabel}
        selectedTag={tag}
        onSelectTag={setTag}
      />

      {error && (
        <p className="rounded-xl border border-danger bg-danger-soft px-4 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      {filtering && (
        <div className="flex items-center justify-between gap-3 rounded-xl border border-line bg-surface-soft px-4 py-2 text-sm text-ink-soft">
          <span>
            걸러서 보는 중입니다. {forCalendar.length}건만 달력에 섰습니다.
          </span>
          <button
            type="button"
            onClick={clearFilters}
            className="shrink-0 rounded-md border border-line px-3 py-1 text-xs transition-colors hover:bg-accent-soft hover:text-accent"
          >
            필터 지우기
          </button>
        </div>
      )}

      <ScheduleCalendar
        month={month}
        onMonthChange={setMonth}
        selectedDate={selectedDate}
        onSelectDate={toggleDate}
        instances={forCalendar}
        loading={loading}
        onPickInstance={setPicked}
        onToggleStatus={handleToggleStatus}
      />

      {picked && (
        <InstanceActionModal
          instance={picked}
          onClose={() => setPicked(null)}
          onToggleStatus={handleToggleStatus}
          onMove={handleMove}
          onSkip={handleSkip}
          onDelete={handleDelete}
        />
      )}
    </div>
  );
}
