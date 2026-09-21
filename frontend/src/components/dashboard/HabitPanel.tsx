import { Link } from "react-router";
import { habitSummary } from "../../domain/dashboard";
import type { Habit } from "../../types/habit";
import DashboardPanel from "./DashboardPanel";

interface Props {
  habits: Habit[];
  today: string;
}

/** 습관 요약. 기록은 습관일지 탭에서 한다 */
export default function HabitPanel({ habits, today }: Props) {
  const { doneToday, total, topStreaks, emptyCategories } = habitSummary(
    habits,
    today,
  );

  const more = (
    <Link to="/habits" className="text-xs text-accent hover:underline">
      습관일지 →
    </Link>
  );

  if (total === 0) {
    return (
      <DashboardPanel title="습관" action={more}>
        <p className="py-6 text-center text-sm text-ink-faint">
          아직 습관이 없습니다. 2분이면 되는 것부터 하나 두어 보세요.
        </p>
      </DashboardPanel>
    );
  }

  return (
    <DashboardPanel title="습관" action={more}>
      <div className="flex items-baseline justify-between text-sm">
        <span className="text-ink-soft">오늘</span>
        <span className="font-semibold text-ink">
          {doneToday} / {total}
        </span>
      </div>

      <div className="flex flex-col gap-1">
        <span className="text-sm text-ink-soft">이어 온 날</span>
        {topStreaks.length === 0 ? (
          <p className="text-xs text-ink-faint">
            어제부터 다시 이어 가면 됩니다.
          </p>
        ) : (
          <ul className="flex list-none flex-col gap-0.5 p-0">
            {topStreaks.map(({ habit, streak }) => (
              <li key={habit.id} className="flex items-center gap-2 text-sm">
                <span className="min-w-0 flex-1 truncate text-ink">
                  {habit.title}
                </span>
                <span className="shrink-0 rounded-full bg-accent-soft px-2 py-0.5 text-xs text-accent">
                  {streak}일째
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {emptyCategories.length > 0 && (
        <p className="text-xs text-ink-faint">
          비어 있는 갈래: {emptyCategories.join(" · ")}
        </p>
      )}
    </DashboardPanel>
  );
}
