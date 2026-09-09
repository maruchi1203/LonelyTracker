import { useEffect, useRef, useState } from "react";
import type { FormVariant } from "../../domain/scheduleForm";
import type { ScheduleCreateRequest } from "../../types/schedule";
import type { ParentOption } from "../schedule/ScheduleFields";
import QuickAddBar from "./QuickAddBar";

interface Props {
  /** 달력에서 고른 날짜. 리스트처럼 날짜 개념이 없는 탭은 주지 않는다 */
  defaultDate?: Date | null;
  knownTags: string[];
  /** 어느 탭의 폼인지. 칸 구성과 검증이 함께 갈린다 */
  variant?: FormVariant;
  parentOptions?: ParentOption[];
  onCreate: (body: ScheduleCreateRequest) => Promise<boolean>;
}

/** 우하단에 떠 있는 일정 추가 입구. 자연어와 수동 입력이 모두 이 안에 있다 */
export default function QuickAddLauncher({
  defaultDate,
  knownTags,
  variant,
  parentOptions,
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
        <div
          role="dialog"
          aria-label="일정 추가"
          className="fixed right-6 bottom-24 z-40 flex max-h-[75vh] w-[30rem] flex-col overflow-y-auto rounded-2xl border border-slate-200 bg-white p-5 shadow-xl"
        >
          <QuickAddBar
            defaultDate={defaultDate}
            knownTags={knownTags}
            variant={variant}
            parentOptions={parentOptions}
            onCreate={onCreate}
            onDone={() => setOpen(false)}
            autoFocus
          />
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-label={open ? "일정 추가 닫기" : "일정 추가 열기"}
        className={`fixed right-6 bottom-6 z-40 flex size-14 items-center justify-center rounded-full text-3xl leading-none text-white shadow-lg transition-all focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100 ${
          open
            ? "rotate-45 bg-slate-500 hover:bg-slate-600"
            : "bg-brand-500 hover:bg-brand-600"
        }`}
      >
        +
      </button>
    </div>
  );
}
