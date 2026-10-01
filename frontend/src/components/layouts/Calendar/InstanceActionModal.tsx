import { useEffect, useState } from "react";
import {
  formatInstanceRange,
  isEarlyDone,
  isMoved,
} from "../../../domain/instance";
import type {
  DeleteScope,
  ScheduleResponse,
  ScheduleStatus,
} from "../../../types/schedule";
import { toLocalInputValue } from "../../../utils/datetime";
import WarpBorder from "../WarpBorder";

const STATUS_LABEL: Record<ScheduleStatus, string> = {
  PLANNED: "예정",
  DONE: "완료",
  SKIPPED: "건너뜀",
};

const ACTION =
  "rounded-md px-3 py-2 text-left text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-40";

interface Props {
  instance: ScheduleResponse;
  onClose: () => void;
  onToggleStatus: (instance: ScheduleResponse) => void;
  onMove: (instance: ScheduleResponse, startAt: string) => void;
  onSkip: (instance: ScheduleResponse) => void;
  onDelete: (instance: ScheduleResponse, scope: DeleteScope) => void;
}

/**
 * 회차 하나를 다루는 창.
 *
 * 달력 칸은 일곱으로 쪼개져 있어 이 조작들을 칸 안에 담을 수 없다.
 * 칸에서는 고르기만 하고, 무엇을 할지는 화면 가운데에서 묻는다
 */
export default function InstanceActionModal({
  instance,
  onClose,
  onToggleStatus,
  onMove,
  onSkip,
  onDelete,
}: Props) {
  const [moving, setMoving] = useState(false);
  const [moveTo, setMoveTo] = useState(() =>
    toLocalInputValue(new Date(instance.startAt ?? Date.now())),
  );

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  const done = instance.status === "DONE";

  /** 누르면 창을 닫는다. 바뀐 결과는 달력에서 바로 보인다 */
  const run = (act: () => void) => {
    act();
    onClose();
  };

  return (
    <div
      role="presentation"
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-center justify-center bg-canvas/70 p-5"
    >
      {/* 창 안을 누른 것은 닫으라는 뜻이 아니다 */}
      <div
        role="dialog"
        aria-modal="true"
        aria-label={instance.title}
        onClick={(e) => e.stopPropagation()}
        className="w-[min(26rem,100%)]"
      >
        <WarpBorder className="flex flex-col gap-4 rounded-2xl bg-surface p-5">
          <header className="flex flex-col gap-1.5">
            <h2 className="text-base font-semibold wrap-break-word text-ink">
              {instance.title}
            </h2>

            <p className="flex flex-wrap items-center gap-1.5 text-xs text-ink-soft">
              {formatInstanceRange(instance)}

              <span className="rounded-full border border-line px-2 py-0.5 text-ink-faint">
                {STATUS_LABEL[instance.status]}
              </span>

              {/* 수행률만 보면 원래 날에 한 사람과 옮겨서 한 사람이 같아 보인다 */}
              {isMoved(instance) && (
                <span
                  title={`원래 ${instance.instanceDate} 예정`}
                  className="rounded-full border border-warn bg-warn-soft px-2 py-0.5 font-medium text-warn"
                >
                  ↻ 옮김
                </span>
              )}

              {/* 분모에 안 들어가므로 수행률에 잡히지 않는다는 것을 알려준다 */}
              {isEarlyDone(instance) && (
                <span
                  title={`${instance.instanceDate} 이 오기 전에 완료했습니다`}
                  className="rounded-full border border-info bg-info-soft px-2 py-0.5 font-medium text-info"
                >
                  ⏱ 조기 종료
                </span>
              )}
            </p>
          </header>

          {moving ? (
            <div className="flex flex-col gap-2 border-t border-line pt-3 text-sm">
              <label className="text-ink-soft" htmlFor="move-to">
                언제로 옮길까요?
              </label>
              <input
                id="move-to"
                type="datetime-local"
                value={moveTo}
                onChange={(e) => setMoveTo(e.target.value)}
                className="rounded-md border border-line bg-surface px-2.5 py-2 text-ink focus:border-accent focus:outline-none focus:ring-3 focus:ring-line"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setMoving(false)}
                  className="rounded-md border border-line px-3 py-1.5 text-sm text-ink-soft hover:bg-surface-soft"
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={() => run(() => onMove(instance, `${moveTo}:00`))}
                  className="rounded-md bg-warn px-3 py-1.5 text-sm font-semibold text-canvas hover:bg-warn/80"
                >
                  옮기기
                </button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-0.5 border-t border-line pt-3">
              <button
                type="button"
                onClick={() => run(() => onToggleStatus(instance))}
                className={`${ACTION} text-ink hover:bg-accent-soft hover:text-accent`}
              >
                {done ? "완료 되돌리기" : "완료로 표시"}
              </button>

              <button
                type="button"
                onClick={() => setMoving(true)}
                disabled={!instance.instanceDate}
                title={
                  instance.instanceDate ? undefined : "날짜가 없는 항목입니다"
                }
                className={`${ACTION} text-ink hover:bg-warn-soft hover:text-warn`}
              >
                다른 날로 옮기기
              </button>

              {/*
                안 한 것을 안 했다고 남긴다. 달성률의 분모에 그대로 남는다.
                지키기로 한 규칙이 있어야 안 지킨 것도 성립하므로 반복에만 둔다
              */}
              {instance.recurring && (
                <button
                  type="button"
                  onClick={() => run(() => onSkip(instance))}
                  className={`${ACTION} text-ink hover:bg-surface-soft`}
                >
                  건너뛰기
                </button>
              )}

              {/* 범위를 버튼 하나로 넘겨짚지 않는다 */}
              <button
                type="button"
                onClick={() => run(() => onDelete(instance, "FUTURE"))}
                className={`${ACTION} text-ink hover:bg-danger-soft hover:text-danger`}
              >
                앞으로 그만두기 (기록 유지)
              </button>

              <button
                type="button"
                onClick={() => run(() => onDelete(instance, "ALL"))}
                className={`${ACTION} text-danger hover:bg-danger-soft`}
              >
                전체 삭제
              </button>
            </div>
          )}
        </WarpBorder>
      </div>
    </div>
  );
}
