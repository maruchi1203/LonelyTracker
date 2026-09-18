import { useCallback, useEffect, useState } from "react";
import { fetchHabits } from "../api/habits";
import {
  changeCompletion,
  changeInstanceStatus,
  fetchScheduleList,
  fetchSchedules,
} from "../api/schedules";
import { fetchAiProviders, fetchAiUsage } from "../api/users";
import AgendaPanel from "../components/dashboard/AgendaPanel";
import { PendingCard } from "../components/dashboard/DashboardCard";
import HabitPanel from "../components/dashboard/HabitPanel";
import UsagePanel from "../components/dashboard/UsagePanel";
import { weekOf } from "../domain/dashboard";
import type { Habit } from "../types/habit";
import type {
  AiProvider,
  AiUsageSummary,
  ScheduleListItem,
  ScheduleResponse,
} from "../types/schedule";
import { toLocalDate } from "../utils/datetime";

/** 연속일을 셀 만큼 거슬러 받는 날 수 */
const HABIT_HISTORY_DAYS = 60;

interface Data {
  instances: ScheduleResponse[];
  items: ScheduleListItem[];
  habits: Habit[];
  usage: AiUsageSummary;
  providers: AiProvider[];
}

export default function DashboardPage() {
  const [data, setData] = useState<Data | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const today = toLocalDate(new Date());

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  const reload = useCallback(async () => {
    const now = toLocalDate(new Date());
    // 오늘에 걸친 기간 일정도 잡히도록 주 전체를 받는다
    const week = weekOf(now);
    const since = new Date(`${now}T00:00:00`);
    since.setDate(since.getDate() - HABIT_HISTORY_DAYS);

    try {
      const [instances, items, habits, usage, providers] = await Promise.all([
        fetchSchedules({ from: `${week.from}T00:00:00`, to: `${week.to}T23:59:59` }),
        fetchScheduleList(),
        fetchHabits(toLocalDate(since), now),
        fetchAiUsage(),
        fetchAiProviders(),
      ]);
      setData({ instances, items, habits, usage, providers: providers.providers });
      setError(null);
    } catch (e) {
      fail(e, "대시보드를 불러오지 못했습니다");
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const run = async (action: () => Promise<unknown>, fallback: string) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      await reload();
    } catch (e) {
      fail(e, fallback);
    } finally {
      setBusy(false);
    }
  };

  const handleToggleInstance = (instance: ScheduleResponse) => {
    const done = instance.status === "DONE";
    // 1회성의 완료는 일정 자체가, 반복의 완료는 회차가 갖는다
    void run(
      () =>
        instance.recurring && instance.instanceDate
          ? changeInstanceStatus(instance.id, instance.instanceDate, done ? "PLANNED" : "DONE")
          : changeCompletion(instance.id, !done),
      "상태를 바꾸지 못했습니다",
    );
  };

  const handleCompleteItem = (item: ScheduleListItem) => {
    void run(() => changeCompletion(item.id, true), "완료하지 못했습니다");
  };

  return (
    <div className="flex flex-col gap-5">
      <h2 className="text-lg font-semibold text-slate-800">대시보드</h2>

      {error && (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </p>
      )}

      {data === null ? (
        <p className="px-5 py-8 text-center text-sm text-slate-400">불러오는 중입니다…</p>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          <UsagePanel usage={data.usage} providers={data.providers} />
          <PendingCard
            title="주간 목표"
            summary="ver.2 에서 만듭니다. 이번 주 목표와 진행을 보여 줍니다."
          />
          <AgendaPanel
            instances={data.instances}
            items={data.items}
            today={today}
            busy={busy}
            onToggleInstance={handleToggleInstance}
            onCompleteItem={handleCompleteItem}
          />
          <HabitPanel habits={data.habits} today={today} />
        </div>
      )}
    </div>
  );
}
