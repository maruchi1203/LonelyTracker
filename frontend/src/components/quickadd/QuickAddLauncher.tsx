import { useEffect, useRef } from "react";
import QuickAddBar from "./QuickAddBar";
import IconButton from "../layouts/IconButton";
import { CloseIcon, PlusIcon } from "../layouts/Icons";
import WarpBorder from "../layouts/WarpBorder";
import { useQuickAdd } from "./QuickAddContext";

/**
 * 우하단에 떠 있는 일정 추가 입구. 자연어와 수동 입력이 모두 이 안에 있다.
 *
 * 화면마다 두지 않고 AppShell 하나에만 둔다. 어느 탭에서 열든 같은 자리이고,
 * 탭을 옮겨도 읽어 둔 초안이 그대로 남는다
 */
export default function QuickAddLauncher() {
  const { open, setOpen, state, variant } = useQuickAdd();

  /** 탭마다 만드는 것이 다르다. 화면을 읽어 주는 도구에게도 그걸 알려야 한다 */
  const what = variant === "habit" ? "습관" : "일정";
  const panel = useRef<HTMLDivElement>(null);

  // 닫아 둔 사이에도 읽고 있다는 것을 단추가 알려준다
  const busy = state.mode === "parsing";

  useEffect(() => {
    if (!open) return;

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    const onPointerDown = (e: PointerEvent) => {
      if (!panel.current?.contains(e.target as Node)) setOpen(false);
    };

    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("pointerdown", onPointerDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("pointerdown", onPointerDown);
    };
  }, [open, setOpen]);

  // 열면 단추가 비켜난다. 패널과 단추가 같은 자리를 놓고 겹치지 않게
  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-haspopup="dialog"
        aria-label={`${what} 추가 열기`}
        className={`fixed right-6 bottom-6 z-40 flex size-14 items-center justify-center rounded-full bg-accent text-canvas shadow-lg transition-colors hover:bg-ink focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line ${
          busy ? "animate-pulse" : ""
        }`}
      >
        <PlusIcon />
      </button>
    );
  }

  return (
    /*
     * 자리잡기를 이 겹이 맡는다. WarpBorder 는 받은 class 앞에 relative 를 붙이는데,
     * Tailwind 가 .fixed 보다 .relative 를 뒤에 찍어 안쪽에 fixed 를 주면 relative 가 이긴다
     */
    <div
      ref={panel}
      className="fixed right-6 bottom-6 z-40 w-[min(32rem,calc(100vw-3rem))]"
    >
      <WarpBorder
        role="dialog"
        aria-label={`${what} 추가`}
        className="flex max-h-[80vh] flex-col gap-2 rounded-2xl bg-surface px-6 py-4"
      >
        <div className="flex justify-end">
          <IconButton label="닫기" onClick={() => setOpen(false)}>
            <CloseIcon />
          </IconButton>
        </div>

        {/* 넘치는 내용만 구른다. 일그러지는 겹이 함께 밀리지 않게 안쪽에 둔다 */}
        <div className="min-h-0 overflow-y-auto">
          <QuickAddBar onDone={() => setOpen(false)} autoFocus />
        </div>
      </WarpBorder>
    </div>
  );
}
