import type { Schedule, ScheduleStatus } from "../types/schedule";

interface Props {
  schedules: Schedule[];
  onToggleStatus: (schedule: Schedule) => void;
  onDelete: (id: number) => void;
}

const STATUS_LABEL: Record<ScheduleStatus, string> = {
  PLANNED: "예정",
  DONE: "완료",
  SKIPPED: "건너뜀",
};

function formatRange(schedule: Schedule): string {
  const start = new Date(schedule.startAt);
  const time = (d: Date) =>
    `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  const date = `${start.getMonth() + 1}/${start.getDate()}`;

  if (!schedule.endAt) return `${date} ${time(start)}`;
  return `${date} ${time(start)} ~ ${time(new Date(schedule.endAt))}`;
}

export default function ScheduleList({
  schedules,
  onToggleStatus,
  onDelete,
}: Props) {
  if (schedules.length === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-12 text-center text-sm text-slate-400">
        등록된 일정이 없습니다. 위에서 첫 일정을 추가해 보세요.
      </p>
    );
  }

  return (
    <ul className="flex list-none flex-col gap-2 p-0">
      {schedules.map((schedule) => {
        const done = schedule.status === "DONE";

        return (
          <li
            key={schedule.id}
            // 왼쪽 띠 색으로 완료 여부를 구분한다
            className={`flex items-center gap-3.5 rounded-xl border border-l-3 border-slate-200 px-4 py-3.5 transition-all hover:border-slate-300 hover:shadow-md ${
              done
                ? "border-l-slate-300 bg-slate-50"
                : "border-l-brand-300 bg-white hover:border-l-brand-500"
            }`}
          >
            <input
              type="checkbox"
              className="size-[1.15rem] shrink-0 cursor-pointer accent-brand-500"
              checked={done}
              onChange={() => onToggleStatus(schedule)}
              aria-label={`${schedule.title} 완료 표시`}
            />

            <div className="flex min-w-0 flex-1 flex-col gap-1">
              <span
                className={`font-medium wrap-break-word ${
                  done ? "text-slate-400 line-through" : "text-slate-800"
                }`}
              >
                {schedule.title}
              </span>

              <span className="flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
                {formatRange(schedule)}

                {schedule.category && (
                  <span className="rounded-full border border-brand-100 bg-brand-50 px-2 py-0.5 font-medium text-brand-700">
                    {schedule.category}
                  </span>
                )}

                <span
                  className={`rounded-full border px-2 py-0.5 ${
                    done
                      ? "border-brand-100 bg-brand-50 text-brand-600"
                      : "border-slate-200 text-slate-400"
                  }`}
                >
                  {STATUS_LABEL[schedule.status]}
                </span>
              </span>
            </div>

            <button
              type="button"
              className="shrink-0 rounded-md border border-transparent px-2.5 py-1.5 text-xs text-slate-400 transition-colors hover:border-red-200 hover:bg-red-50 hover:text-red-600 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100"
              onClick={() => onDelete(schedule.id)}
            >
              삭제
            </button>
          </li>
        );
      })}
    </ul>
  );
}
