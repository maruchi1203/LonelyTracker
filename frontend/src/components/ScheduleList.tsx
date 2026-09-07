import { useEffect, useRef, useState } from "react";
import {
  formatInstanceRange,
  instanceKey,
  isEarlyDone,
  isMoved,
} from "../domain/instance";
import type {
  DeleteScope,
  ScheduleResponse,
  ScheduleStatus,
} from "../types/schedule";
import { toLocalInputValue } from "../utils/datetime";

interface Props {
  instances: ScheduleResponse[];
  onToggleStatus: (instance: ScheduleResponse) => void;
  onMove: (instance: ScheduleResponse, startAt: string) => void;
  onSkip: (instance: ScheduleResponse) => void;
  onDelete: (instance: ScheduleResponse, scope: DeleteScope) => void;
  /** 비어 있는 이유. 일정이 없는 것과 필터에 걸린 것은 다르다 */
  emptyReason?: "no-data" | "filtered-out";
  onClearFilters?: () => void;
}

const STATUS_LABEL: Record<ScheduleStatus, string> = {
  PLANNED: "예정",
  DONE: "완료",
  SKIPPED: "건너뜀",
};

const MENU_ITEM =
  "w-full rounded-md px-3 py-1.5 text-left text-xs whitespace-nowrap transition-colors";

export default function ScheduleList({
  instances,
  onToggleStatus,
  onMove,
  onSkip,
  onDelete,
  emptyReason = "no-data",
  onClearFilters,
}: Props) {
  if (instances.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-12 text-center text-sm text-slate-400">
        {emptyReason === "filtered-out" ? (
          <>
            <p>이 조건에 맞는 일정이 없습니다.</p>
            {onClearFilters && (
              <button
                type="button"
                onClick={onClearFilters}
                className="rounded-md border border-slate-200 px-3 py-1.5 text-xs text-slate-600 hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700"
              >
                필터 지우기
              </button>
            )}
          </>
        ) : (
          <p>등록된 일정이 없습니다. 오른쪽 아래 + 로 추가해 보세요.</p>
        )}
      </div>
    );
  }

  return (
    <ul className="flex list-none flex-col gap-2 p-0">
      {instances.map((instance) => (
        <ScheduleListItem
          key={instanceKey(instance)}
          instance={instance}
          onToggleStatus={onToggleStatus}
          onMove={onMove}
          onSkip={onSkip}
          onDelete={onDelete}
        />
      ))}
    </ul>
  );
}

type ItemProps = Pick<
  Props,
  "onToggleStatus" | "onMove" | "onSkip" | "onDelete"
> & { instance: ScheduleResponse };

function ScheduleListItem({
  instance,
  onToggleStatus,
  onMove,
  onSkip,
  onDelete,
}: ItemProps) {
  const [pane, setPane] = useState<"none" | "menu" | "move">("none");
  const [moveTo, setMoveTo] = useState(() =>
    toLocalInputValue(new Date(instance.startAt)),
  );
  const row = useRef<HTMLLIElement>(null);

  useEffect(() => {
    if (pane !== "menu") return;

    const close = (e: Event) => {
      if (e instanceof KeyboardEvent && e.key !== "Escape") return;
      if (e.type === "pointerdown" && row.current?.contains(e.target as Node)) {
        return;
      }
      setPane("none");
    };

    document.addEventListener("keydown", close);
    document.addEventListener("pointerdown", close);
    return () => {
      document.removeEventListener("keydown", close);
      document.removeEventListener("pointerdown", close);
    };
  }, [pane]);

  const done = instance.status === "DONE";

  return (
    <li
      ref={row}
      // 우클릭도 같은 메뉴를 연다. 자리는 항상 같아야 다음에 어디를 볼지 안다
      onContextMenu={(e) => {
        e.preventDefault();
        setPane("menu");
      }}
      // 왼쪽 띠 색으로 완료 여부를 구분한다
      className={`relative flex flex-col gap-2 rounded-xl border border-l-3 border-slate-200 px-4 py-3.5 transition-all hover:border-slate-300 hover:shadow-md ${
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
          onChange={() => onToggleStatus(instance)}
          aria-label={`${instance.title} 완료 표시`}
        />

        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <span
            className={`font-medium wrap-break-word ${
              done ? "text-slate-400 line-through" : "text-slate-800"
            }`}
          >
            {instance.title}
          </span>

          <span className="flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
            {formatInstanceRange(instance)}

            {instance.category && (
              <span className="rounded-full border border-brand-100 bg-brand-50 px-2 py-0.5 font-medium text-brand-700">
                {instance.category}
              </span>
            )}

            <span
              className={`rounded-full border px-2 py-0.5 ${
                done
                  ? "border-brand-100 bg-brand-50 text-brand-600"
                  : "border-slate-200 text-slate-400"
              }`}
            >
              {STATUS_LABEL[instance.status]}
            </span>

            {/* 수행률만 보면 원래 날에 한 사람과 옮겨서 한 사람이 같아 보인다 */}
            {isMoved(instance) && (
              <span
                className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 font-medium text-amber-700"
                title={`원래 ${instance.instanceDate} 예정`}
              >
                ↻ 옮김
              </span>
            )}

            {/* 분모에 안 들어가므로 수행률에 잡히지 않는다는 것을 알려준다 */}
            {isEarlyDone(instance) && (
              <span
                className="rounded-full border border-sky-200 bg-sky-50 px-2 py-0.5 font-medium text-sky-700"
                title={`${instance.instanceDate} 이 오기 전에 완료했습니다`}
              >
                ⏱ 조기 종료
              </span>
            )}
          </span>
        </div>

        <button
          type="button"
          aria-label={`${instance.title} 작업 메뉴`}
          aria-expanded={pane === "menu"}
          onClick={() => setPane((p) => (p === "menu" ? "none" : "menu"))}
          className="shrink-0 rounded-md border border-transparent px-2.5 py-1.5 text-slate-400 transition-colors hover:border-slate-200 hover:bg-slate-50 hover:text-slate-700 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100"
        >
          ⋯
        </button>
      </div>

      {pane === "menu" && (
        <div
          role="menu"
          className="absolute top-12 right-3 z-20 flex w-52 flex-col gap-0.5 rounded-xl border border-slate-200 bg-white p-1.5 shadow-lg"
        >
          <button
            type="button"
            role="menuitem"
            className={`${MENU_ITEM} text-slate-700 hover:bg-amber-50 hover:text-amber-700`}
            onClick={() => setPane("move")}
          >
            다른 날로 옮기기
          </button>

          {/* 안 한 것을 안 했다고 남긴다. 수행률의 분모에 그대로 남는다 */}
          <button
            type="button"
            role="menuitem"
            className={`${MENU_ITEM} text-slate-700 hover:bg-slate-100`}
            onClick={() => {
              setPane("none");
              onSkip(instance);
            }}
          >
            건너뛰기
          </button>

          {/* 범위를 버튼 하나로 넘겨짚지 않는다 */}
          <button
            type="button"
            role="menuitem"
            className={`${MENU_ITEM} text-slate-700 hover:bg-red-50 hover:text-red-600`}
            onClick={() => {
              setPane("none");
              onDelete(instance, "FUTURE");
            }}
          >
            앞으로 그만두기 (기록 유지)
          </button>

          <button
            type="button"
            role="menuitem"
            className={`${MENU_ITEM} text-red-600 hover:bg-red-50`}
            onClick={() => {
              setPane("none");
              onDelete(instance, "ALL");
            }}
          >
            전체 삭제
          </button>
        </div>
      )}

      {pane === "move" && (
        <div className="flex flex-wrap items-center gap-2 border-t border-slate-100 pt-2 text-xs">
          <label
            className="text-slate-500"
            htmlFor={`to-${instanceKey(instance)}`}
          >
            언제로 옮길까요?
          </label>
          <input
            id={`to-${instanceKey(instance)}`}
            type="datetime-local"
            className="rounded-md border border-slate-200 px-2 py-1 text-slate-800 focus:border-brand-500 focus:outline-none"
            value={moveTo}
            onChange={(e) => setMoveTo(e.target.value)}
          />
          <button
            type="button"
            className="rounded-md bg-amber-500 px-3 py-1 font-semibold text-white hover:bg-amber-600"
            onClick={() => {
              onMove(instance, `${moveTo}:00`);
              setPane("none");
            }}
          >
            옮기기
          </button>
          <button
            type="button"
            className="rounded-md border border-slate-200 px-3 py-1 text-slate-500 hover:bg-slate-50"
            onClick={() => setPane("none")}
          >
            취소
          </button>
        </div>
      )}
    </li>
  );
}
