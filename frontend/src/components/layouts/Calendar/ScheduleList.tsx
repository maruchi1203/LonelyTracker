import { instanceKey } from "../../../domain/instance";
import type { DeleteScope, ScheduleResponse } from "../../../types/schedule";
import WarpBorder from "../WarpBorder";
import ScheduleListItem from "./ScheduleListItem";

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
      <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-line bg-surface px-4 py-12 text-center text-sm text-ink-faint">
        {emptyReason === "filtered-out" ? (
          <>
            <p>이 조건에 맞는 일정이 없습니다.</p>
            {onClearFilters && (
              <button
                type="button"
                onClick={onClearFilters}
                className="rounded-md border border-line px-3 py-1.5 text-xs text-ink-soft hover:border-line hover:bg-accent-soft hover:text-accent"
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
    <WarpBorder className="rounded-2xl p-2">
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
    </WarpBorder>
  );
}
