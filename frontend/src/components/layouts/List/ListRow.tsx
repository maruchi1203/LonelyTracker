import React, {
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { describeRecurrence, stepOccurrence } from "../../../domain/recurrence";
import {
  type DropIntent,
  INDENT_PX,
  dropIntentAt,
  dropLevelAt,
} from "../../../domain/scheduleDrop";
import { isDueSoon, isOccurrenceDone } from "../../../domain/scheduleTree";
import type {
  ScheduleListItem,
  SchedulePriority,
} from "../../../types/schedule";

const MENU_ITEM =
  "rounded-md px-2.5 py-1.5 text-left text-sm transition-colors";

/** 값이 없으면 뱃지를 달지 않는다. 정렬에서만 선택으로 본다 */
const PRIORITY_BADGE: Record<
  SchedulePriority,
  { label: string; style: string }
> = {
  MUST: { label: "필수", style: "bg-danger-soft text-danger" },
  SHOULD: { label: "권장", style: "bg-accent-soft text-accent" },
  COULD: { label: "선택", style: "bg-surface-soft text-ink-soft" },
  WONT: { label: "보류", style: "bg-surface-soft text-ink-faint" },
};

interface RowProps {
  item: ScheduleListItem;
  /** 이 행이 선 단. 드롭 표시선을 목록 기준으로 되돌리는 데 쓴다 */
  depth: number;
  shownOn?: string;
  onStep: (to: string) => void;
  onRewind: () => void;
  onToggle: (onDate?: string) => void;
  onEdit: () => void;
  onDelete: () => void;
  draggable: boolean;
  dragging: boolean;
  drop: { intent: DropIntent; level: number } | null;
  onDragStart: () => void;
  onDragEnd: () => void;
  onDragOverRow: (intent: DropIntent, level: number) => void;
  onDropAt: () => void;
  /** 이 행의 하위 행들. 같은 액자 안에 들어간다 */
  children?: ReactNode;
}

/** 회차를 앞뒤로 옮기는 화살표. 규칙 밖이면 눌리지 않는다 */
function StepButton({
  label,
  to,
  onStep,
  children,
}: {
  label: string;
  to: string | null;
  onStep: (to: string) => void;
  children: string;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={to === null}
      onClick={() => to !== null && onStep(to)}
      className="rounded px-1 leading-none transition-colors enabled:hover:bg-accent-soft disabled:text-ink-faint"
    >
      {children}
    </button>
  );
}

/**
 * 커서 자리를 읽는다. 위아래가 뜻을, 좌우가 단을 정한다
 *
 * 단은 행이 아니라 목록 왼쪽 끝에서 잰다. 행은 중첩된 깊이만큼 이미 밀려 있어
 * 제 왼쪽 끝을 기준 삼으면 어느 단에 있든 0 으로 읽힌다
 */
function spotAtPointer(e: React.DragEvent): [DropIntent, number] {
  const box = e.currentTarget.getBoundingClientRect();
  const root = e.currentTarget.closest("[data-list-root]");
  const left = root?.getBoundingClientRect().left ?? box.left;
  return [
    dropIntentAt((e.clientY - box.top) / box.height),
    dropLevelAt(e.clientX - left),
  ];
}

export default function ListRow({
  item,
  depth,
  shownOn,
  onStep,
  onRewind,
  onToggle,
  onEdit,
  onDelete,
  draggable,
  dragging,
  drop,
  onDragStart,
  onDragEnd,
  onDragOverRow,
  onDropAt,
  children,
}: RowProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const row = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;

    const close = (e: Event) => {
      if (e instanceof KeyboardEvent && e.key !== "Escape") return;
      // 메뉴 항목을 누른 것이면 닫기와 동작이 서로 싸운다
      if (e.type === "pointerdown" && row.current?.contains(e.target as Node)) {
        return;
      }
      setMenuOpen(false);
    };

    document.addEventListener("keydown", close);
    document.addEventListener("pointerdown", close);
    return () => {
      document.removeEventListener("keydown", close);
      document.removeEventListener("pointerdown", close);
    };
  }, [menuOpen]);

  const done = isOccurrenceDone(item, shownOn);
  // 안 하기로 한 일정. 지우지 않고 판단을 기록으로 남긴다
  const shelved = item.priority === "WONT";
  const badge = item.priority ? PRIORITY_BADGE[item.priority] : null;

  /*
   * 재는 때를 그릴 때마다 잡는다. 화면을 열어 둔 채로 시각이 넘어가면
   * 다음에 다시 그려질 때 켜진다 — 초 단위로 지켜볼 일은 아니다
   */
  const soon = isDueSoon(item, new Date());

  return (
    <li className="list-none">
      {/* 행은 평범한 테두리를 쓴다. 일렁이는 액자는 목록 바깥에 하나뿐이다 */}
      <div className="rounded-2xl border border-line bg-surface shadow-xs">
        <div
          ref={row}
          onContextMenu={(e) => {
            e.preventDefault();
            setMenuOpen(true);
          }}
          /* 하위 행 위에서 시작한 끌기가 부모까지 올라오면 늘 부모가 이긴다 */
          onDragOver={(e) => {
            if (!draggable) return;
            e.preventDefault();
            e.stopPropagation();
            onDragOverRow(...spotAtPointer(e));
          }}
          onDrop={(e) => {
            e.preventDefault();
            e.stopPropagation();
            onDropAt();
          }}
          className={`relative flex items-start gap-2 px-3 py-3 transition-opacity ${
            dragging ? "opacity-40" : ""
          } ${shelved ? "opacity-50" : ""} ${
            drop?.intent === "inside"
              ? "bg-accent-soft ring-2 ring-inset ring-line"
              : ""
          }`}
        >
          {/*
            어느 틈에 몇 단으로 설지를 선의 자리로 보여준다
            행이 이미 depth 만큼 밀려 있어 그만큼 빼야 목록 기준 자리에 선다
          */}
          {drop !== null && drop.intent !== "inside" && (
            <span
              aria-hidden
              style={{ marginLeft: (drop.level - depth) * INDENT_PX }}
              className={`pointer-events-none absolute right-3 left-3 h-0.5 rounded-full bg-accent ${
                drop.intent === "before" ? "top-0" : "bottom-0"
              }`}
            />
          )}

          {/* 손잡이만 끈다. 행 전체를 끌면 글자를 고르는 것과 부딪힌다 */}
          <button
            type="button"
            draggable={draggable}
            onDragStart={onDragStart}
            onDragEnd={onDragEnd}
            disabled={!draggable}
            title={
              draggable
                ? "끌어서 자리 바꾸기. 오른쪽으로 밀면 위 일정의 하위로 들어갑니다"
                : "내 순서로 볼 때만 자리를 바꿀 수 있습니다"
            }
            aria-label={`${item.title} 순서 바꾸기`}
            className={`mt-0.5 shrink-0 px-1 ${
              draggable
                ? "cursor-grab text-ink-faint hover:text-ink-soft active:cursor-grabbing"
                : "cursor-not-allowed text-ink-faint"
            }`}
          >
            ⠿
          </button>

          {/* 반복은 회차 하나만 걸려 있다. 끝내면 다음 회차가 올라와 늘 비어 보인다 */}
          <input
            type="checkbox"
            checked={done}
            onChange={() => onToggle(shownOn)}
            disabled={item.recurring && shownOn === undefined}
            aria-label={`${item.title} 완료`}
            className="mt-1 size-4 shrink-0 accent-accent"
          />

          <div className="flex min-w-0 flex-1 flex-col gap-1">
            <div className="flex min-w-0 items-center gap-2">
              <button
                type="button"
                onClick={onEdit}
                className={`min-w-0 truncate text-left text-sm hover:underline ${
                  done ? "text-ink-faint line-through" : "text-ink"
                }`}
              >
                {item.title}
              </button>

              {/*
                하루 안에 끝내야 하는 일이 이름 옆에서 맥을 친다.
                이름 앞이 아니라 뒤에 두는 것은, 앞에 두면 점 없는 행과 제목 왼쪽이 어긋나서다
              */}
              {soon && (
                <span
                  title="하루 안에 끝내야 합니다"
                  className="size-2 shrink-0 animate-beat rounded-full bg-danger"
                />
              )}
            </div>

            <div className="flex flex-wrap items-center gap-1.5 text-xs text-ink-faint">
              {badge && (
                <span className={`rounded-full px-2 py-0.5 ${badge.style}`}>
                  {badge.label}
                </span>
              )}
              {item.recurring && item.recurrence && (
                <span className="flex items-center gap-1 text-accent">
                  ⟳ {describeRecurrence(item.recurrence)}
                  {shownOn === undefined ? (
                    "· 남은 회차 없음"
                  ) : (
                    <>
                      <StepButton
                        label={`${item.title} 이전 회차`}
                        to={stepOccurrence(item.recurrence, shownOn, -1)}
                        onStep={onStep}
                      >
                        ‹
                      </StepButton>
                      <span className="tabular-nums">{shownOn}</span>
                      <button
                        type="button"
                        aria-label={`${item.title} 지금 할 회차로`}
                        title="지금 할 회차로 돌아가기"
                        disabled={shownOn === item.occurrenceOn}
                        onClick={onRewind}
                        className="leading-none transition-colors enabled:text-accent enabled:hover:text-accent disabled:text-ink-faint"
                      >
                        ◉
                      </button>
                      <StepButton
                        label={`${item.title} 다음 회차`}
                        to={stepOccurrence(item.recurrence, shownOn, 1)}
                        onStep={onStep}
                      >
                        ›
                      </StepButton>
                    </>
                  )}
                </span>
              )}
              {item.dueOn && <span>기한 {item.dueOn}</span>}
              {item.startAt && <span>시작 {item.startAt.slice(0, 10)}</span>}
              {item.place && <span>{item.place}</span>}
              {item.tags?.map((tag) => (
                <span
                  key={tag}
                  className="rounded-full bg-accent-soft px-2 py-0.5 text-accent"
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>

          <button
            type="button"
            aria-label={`${item.title} 작업 메뉴`}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
            className="shrink-0 rounded-md border border-transparent px-2 py-1 text-ink-faint transition-colors hover:border-line hover:bg-surface-soft hover:text-ink focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line"
          >
            ⋯
          </button>

          {menuOpen && (
            <div
              role="menu"
              className="absolute top-10 right-3 z-20 flex w-40 flex-col gap-0.5 rounded-xl border border-line bg-surface p-1.5 shadow-lg"
            >
              <button
                type="button"
                role="menuitem"
                className={`${MENU_ITEM} text-ink hover:bg-surface-soft`}
                onClick={() => {
                  setMenuOpen(false);
                  onEdit();
                }}
              >
                수정
              </button>

              <button
                type="button"
                role="menuitem"
                className={`${MENU_ITEM} text-danger hover:bg-danger-soft`}
                onClick={() => {
                  setMenuOpen(false);
                  onDelete();
                }}
              >
                삭제
              </button>
            </div>
          )}
        </div>

        {/* 하위 행. 한 단이 INDENT_PX 와 같아야 손과 눈이 맞는다 */}
        {children && (
          <ul className="flex flex-col gap-2 pr-2 pb-2 pl-8">{children}</ul>
        )}
      </div>
    </li>
  );
}
