import { useState } from "react";
import { occurrenceKey } from "../domain/occurrence";
import type {
  DeleteScope,
  ScheduleResponse,
  ScheduleStatus,
} from "../types/schedule";
import { formatTime, toLocalInputValue } from "../utils/datetime";

interface Props {
  occurrences: ScheduleResponse[];
  onToggleStatus: (occurrence: ScheduleResponse) => void;
  onPostpone: (occurrence: ScheduleResponse, to: string) => void;
  onDelete: (occurrence: ScheduleResponse, scope: DeleteScope) => void;
}

const STATUS_LABEL: Record<ScheduleStatus, string> = {
  PLANNED: "예정",
  DONE: "완료",
  SKIPPED: "건너뜀",
};

const ACTION =
  "shrink-0 rounded-md border border-transparent px-2.5 py-1.5 text-xs text-slate-400 transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100";

function formatRange(occurrence: ScheduleResponse): string {
  const start = new Date(occurrence.startAt);
  const date = `${start.getMonth() + 1}/${start.getDate()}`;

  if (!occurrence.endAt) return `${date} ${formatTime(start)}`;
  return `${date} ${formatTime(start)} ~ ${formatTime(new Date(occurrence.endAt))}`;
}

export default function ScheduleList({
  occurrences,
  onToggleStatus,
  onPostpone,
  onDelete,
}: Props) {
  if (occurrences.length === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-12 text-center text-sm text-slate-400">
        등록된 일정이 없습니다. 위에서 첫 일정을 추가해 보세요.
      </p>
    );
  }

  return (
    <ul className="flex list-none flex-col gap-2 p-0">
      {occurrences.map((occurrence) => (
        <ScheduleListItem
          key={occurrenceKey(occurrence)}
          occurrence={occurrence}
          onToggleStatus={onToggleStatus}
          onPostpone={onPostpone}
          onDelete={onDelete}
        />
      ))}
    </ul>
  );
}

type ItemProps = Omit<Props, "occurrences"> & { occurrence: ScheduleResponse };

function ScheduleListItem({
  occurrence,
  onToggleStatus,
  onPostpone,
  onDelete,
}: ItemProps) {
  const [pane, setPane] = useState<"none" | "postpone" | "delete">("none");
  const [moveTo, setMoveTo] = useState(() =>
    toLocalInputValue(new Date(occurrence.startAt)),
  );

  const done = occurrence.status === "DONE";
  const toggle = (next: "postpone" | "delete") =>
    setPane((p) => (p === next ? "none" : next));

  return (
    <li
      // 왼쪽 띠 색으로 완료 여부를 구분한다
      className={`flex flex-col gap-2 rounded-xl border border-l-3 border-slate-200 px-4 py-3.5 transition-all hover:border-slate-300 hover:shadow-md ${
        done
          ? "border-l-slate-300 bg-slate-50"
          : "border-l-brand-300 bg-white hover:border-l-brand-500"
      }`}
    >
      <div className="flex items-center gap-3.5">
        <input
          type="checkbox"
          className="size-[1.15rem] shrink-0 cursor-pointer accent-brand-500"
          checked={done}
          onChange={() => onToggleStatus(occurrence)}
          aria-label={`${occurrence.title} 완료 표시`}
        />

        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <span
            className={`font-medium wrap-break-word ${
              done ? "text-slate-400 line-through" : "text-slate-800"
            }`}
          >
            {occurrence.title}
          </span>

          <span className="flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
            {formatRange(occurrence)}

            {occurrence.category && (
              <span className="rounded-full border border-brand-100 bg-brand-50 px-2 py-0.5 font-medium text-brand-700">
                {occurrence.category}
              </span>
            )}

            <span
              className={`rounded-full border px-2 py-0.5 ${
                done
                  ? "border-brand-100 bg-brand-50 text-brand-600"
                  : "border-slate-200 text-slate-400"
              }`}
            >
              {STATUS_LABEL[occurrence.status]}
            </span>

            {/* 수행률만 보면 미루는 사람과 계획대로 하는 사람이 같아 보인다 */}
            {occurrence.postponeCount > 0 && (
              <span
                className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 font-medium text-amber-700"
                title={`원래 ${occurrence.occurrenceDate} 예정`}
              >
                ↻ {occurrence.postponeCount}번 미룸
              </span>
            )}
          </span>
        </div>

        <button
          type="button"
          className={`${ACTION} hover:border-amber-200 hover:bg-amber-50 hover:text-amber-700`}
          aria-expanded={pane === "postpone"}
          onClick={() => toggle("postpone")}
        >
          연기
        </button>

        <button
          type="button"
          className={`${ACTION} hover:border-red-200 hover:bg-red-50 hover:text-red-600`}
          aria-expanded={pane === "delete"}
          onClick={() => toggle("delete")}
        >
          삭제
        </button>
      </div>

      {pane === "postpone" && (
        <div className="flex flex-wrap items-center gap-2 border-t border-slate-100 pt-2 text-xs">
          <label className="text-slate-500" htmlFor={`to-${occurrenceKey(occurrence)}`}>
            언제로 미룰까요?
          </label>
          <input
            id={`to-${occurrenceKey(occurrence)}`}
            type="datetime-local"
            className="rounded-md border border-slate-200 px-2 py-1 text-slate-800 focus:border-brand-500 focus:outline-none"
            value={moveTo}
            onChange={(e) => setMoveTo(e.target.value)}
          />
          <button
            type="button"
            className="rounded-md bg-amber-500 px-3 py-1 font-semibold text-white hover:bg-amber-600"
            onClick={() => {
              onPostpone(occurrence, `${moveTo}:00`);
              setPane("none");
            }}
          >
            미루기
          </button>
        </div>
      )}

      {pane === "delete" && (
        <div className="flex flex-wrap items-center gap-2 border-t border-slate-100 pt-2 text-xs">
          {/* 범위를 버튼 하나로 넘겨짚지 않는다 */}
          <span className="text-slate-500">어디까지 지울까요?</span>
          <button
            type="button"
            className="rounded-md border border-slate-200 px-3 py-1 text-slate-600 hover:border-red-200 hover:bg-red-50 hover:text-red-600"
            onClick={() => onDelete(occurrence, "FUTURE")}
          >
            앞으로 그만두기 (기록 유지)
          </button>
          <button
            type="button"
            className="rounded-md bg-red-500 px-3 py-1 font-semibold text-white hover:bg-red-600"
            onClick={() => onDelete(occurrence, "ALL")}
          >
            전체 삭제
          </button>
        </div>
      )}
    </li>
  );
}
