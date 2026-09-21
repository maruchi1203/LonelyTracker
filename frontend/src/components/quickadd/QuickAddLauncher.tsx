import { useEffect, useRef, useState } from "react";
import type { FormVariant } from "../../domain/scheduleForm";
import type { ScheduleCreateRequest } from "../../types/schedule";
import QuickAddBar from "./QuickAddBar";
import WarpBorder from "../layouts/WarpBorder";

interface Props {
  /** 달력에서 고른 날짜. 리스트처럼 날짜 개념이 없는 탭은 주지 않는다 */
  defaultDate?: Date | null;
  knownTags: string[];
  /** 어느 탭의 폼인지. 날짜를 요구할지가 갈린다 */
  variant?: FormVariant;
  onCreate: (body: ScheduleCreateRequest) => Promise<boolean>;
}

/** 우하단에 떠 있는 일정 추가 입구. 자연어와 수동 입력이 모두 이 안에 있다 */
export default function QuickAddLauncher({
  defaultDate,
  knownTags,
  variant,
  onCreate,
}: Props) {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    // 패널 안을 눌렀을 때는 닫지 않는다. 버튼도 이 안에 있어 토글이 두 번 걸리지 않는다
    const onPointerDown = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false);
    };

    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("pointerdown", onPointerDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("pointerdown", onPointerDown);
    };
  }, [open]);

  return (
    <div ref={root}>
      {open && (
        <WarpBorder
          role="dialog"
          aria-label="일정 추가"
          className="fixed right-6 bottom-24 z-40 flex max-h-[75vh] w-120 flex-col rounded-2xl bg-surface p-5 text-line"
        >
          {/* 넘치는 내용만 구른다. 일그러지는 겹이 함께 밀리지 않게 안쪽에 둔다 */}
          <div className="min-h-0 overflow-y-auto">
            <QuickAddBar
              defaultDate={defaultDate}
              knownTags={knownTags}
              variant={variant}
              onCreate={onCreate}
              onDone={() => setOpen(false)}
              autoFocus
            />
          </div>
        </WarpBorder>
      )}

      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-label={open ? "일정 추가 닫기" : "일정 추가 열기"}
        className={`fixed right-6 bottom-6 z-40 flex size-14 items-center justify-center border text-3xl leading-none text-canvas shadow-lg transition-all focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line ${
          open
            ? "rotate-45 bg-ink-soft hover:bg-ink"
            : "bg-accent hover:bg-ink"
        }`}
      >
        {open ? "!!!" : "..."}
      </button>
    </div>
  );
}
