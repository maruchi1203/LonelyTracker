import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type DragEvent,
} from "react";
import {
  changeCompletion,
  createSchedule,
  deleteSchedule,
  fetchScheduleList,
  fetchTagNames,
  reorderSchedules,
} from "../api/schedules";
import QuickAddLauncher from "../components/quickadd/QuickAddLauncher";
import ScheduleEditModal from "../components/schedule/ScheduleEditModal";
import {
  dropIntentAt,
  planDrop,
  type DropIntent,
} from "../domain/scheduleDrop";
import { buildTree, flatten, type ListSort } from "../domain/scheduleTree";
import type {
  ScheduleCreateRequest,
  ScheduleListItem,
  SchedulePriority,
} from "../types/schedule";

/** 깊이만큼 들여쓴다. 계층은 3단까지라 세 칸이면 된다 */
const INDENT = ["", "pl-6", "pl-12"];

const MENU_ITEM =
  "rounded-md px-2.5 py-1.5 text-left text-sm transition-colors";

/** 놓으면 어떻게 되는지 보여주는 표시. 가운데는 자식으로 들어간다 */
const DROP_MARK: Record<DropIntent, string> = {
  before: "border-t-2 border-t-brand-500",
  after: "border-b-2 border-b-brand-500",
  inside: "bg-brand-50 ring-2 ring-inset ring-brand-300",
};

const TOGGLE = "rounded-md border px-3 py-1 text-sm transition-colors";
const TOGGLE_ON = "border-brand-500 bg-brand-500 text-white";
const TOGGLE_OFF = "border-slate-200 text-slate-600 hover:bg-brand-50";

const SORTS: { value: ListSort; label: string }[] = [
  { value: "manual", label: "내 순서" },
  { value: "due", label: "기한순" },
  { value: "priority", label: "우선순위순" },
];

/** 값이 없으면 뱃지를 달지 않는다. 정렬에서만 선택으로 본다 */
const PRIORITY_BADGE: Record<SchedulePriority, { label: string; style: string }> =
  {
    MUST: { label: "필수", style: "bg-red-50 text-red-600" },
    SHOULD: { label: "권장", style: "bg-brand-50 text-brand-700" },
    COULD: { label: "선택", style: "bg-slate-100 text-slate-500" },
    WONT: { label: "보류", style: "bg-slate-100 text-slate-400" },
  };

export default function ListPage() {
  const [items, setItems] = useState<ScheduleListItem[]>([]);
  const [knownTags, setKnownTags] = useState<string[]>([]);
  const [sort, setSort] = useState<ListSort>("manual");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [draggingId, setDraggingId] = useState<number | null>(null);
  const [dropAt, setDropAt] = useState<
    { id: number; intent: DropIntent } | null
  >(null);

  // 보이는 차례와 저장되는 차례가 다르면 놓은 자리와 결과가 어긋난다
  const canDrag = sort === "manual";
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await fetchScheduleList());
      setError(null);
    } catch (e) {
      fail(e, "목록을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadTags = useCallback(async () => {
    try {
      setKnownTags(await fetchTagNames());
    } catch {
      // 목록 쪽에서 이미 에러를 보여주므로 여기서는 조용히 넘어간다
    }
  }, []);

  useEffect(() => {
    void reload();
    void loadTags();
  }, [reload, loadTags]);

  const rows = useMemo(() => flatten(buildTree(items, sort)), [items, sort]);


  /** 성공 여부를 돌려준다. 실패했는데 입력이 지워지면 곤란하다 */
  const handleCreate = async (body: ScheduleCreateRequest): Promise<boolean> => {
    setError(null);
    try {
      await createSchedule(body);

      // 3단을 넘기면 서버가 눌러 앉힌다. 응답 하나만 믿으면 화면이 거짓말한다
      await Promise.all([reload(), loadTags()]);
      return true;
    } catch (e) {
      fail(e, "항목을 추가하지 못했습니다");
      return false;
    }
  };

  const handleToggle = async (item: ScheduleListItem) => {
    setError(null);
    try {
      await changeCompletion(item.id, !item.completedAt);
      await reload();
    } catch (e) {
      fail(e, "완료 상태를 바꾸지 못했습니다");
    }
  };

  /** 끌어다 놓은 자리대로 무리를 다시 세운다 */
  const handleDrop = async (targetId: number, intent: DropIntent) => {
    const from = draggingId;
    setDraggingId(null);
    setDropAt(null);
    if (from === null) return;

    const plan = planDrop(items, from, targetId, intent);
    if (plan === null) {
      setError("그 자리에는 놓을 수 없습니다");
      return;
    }

    setError(null);
    try {
      await reorderSchedules(plan.parentId, plan.ids);

      // 자손이 넘치면 서버가 끌어올린다. 응답 하나만 믿으면 화면이 거짓말한다
      await reload();
    } catch (e) {
      fail(e, "순서를 바꾸지 못했습니다");
    }
  };

  const handleDelete = async (item: ScheduleListItem) => {
    if (!window.confirm(`"${item.title}" 을(를) 지울까요? 되돌릴 수 없습니다.`)) {
      return;
    }

    setError(null);
    try {
      // 리스트에는 습관이 없어 범위가 갈리지 않는다
      await deleteSchedule(item.id, "ALL");

      // 딸린 자식은 서버가 최상위로 올린다. 목록을 다시 읽어야 자리가 맞는다
      await reload();
    } catch (e) {
      fail(e, "지우지 못했습니다");
    }
  };

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-slate-800">리스트</h2>

        <div className="flex items-center gap-1.5">
          {SORTS.map(({ value, label }) => (
            <button
              key={value}
              type="button"
              onClick={() => setSort(value)}
              aria-pressed={sort === value}
              className={`${TOGGLE} ${sort === value ? TOGGLE_ON : TOGGLE_OFF}`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <p className="text-xs text-slate-400">
        날짜를 안 정한 일도 적어 둘 수 있습니다. 끌어서 행의 가운데에 놓으면 그
        일정의 하위로 들어갑니다.
      </p>

      {error && (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </p>
      )}

      <section className="rounded-2xl border border-slate-200 bg-white shadow-xs">
        {rows.length === 0 ? (
          <p className="px-5 py-8 text-center text-sm text-slate-400">
            {loading ? "불러오는 중입니다…" : "아직 적어 둔 것이 없습니다."}
          </p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {rows.map(({ item, depth }) => (
              <ListRow
                key={item.id}
                item={item}
                depth={depth}
                onToggle={() => void handleToggle(item)}
                onEdit={() => setEditingId(item.id)}
                onDelete={() => void handleDelete(item)}
                draggable={canDrag}
                dragging={draggingId === item.id}
                dropIntent={dropAt?.id === item.id ? dropAt.intent : null}
                onDragStart={() => setDraggingId(item.id)}
                onDragEnd={() => {
                  setDraggingId(null);
                  setDropAt(null);
                }}
                onDragOverRow={(intent) => setDropAt({ id: item.id, intent })}
                onDropAt={(intent) => void handleDrop(item.id, intent)}
              />
            ))}
          </ul>
        )}
      </section>

      {/* 다른 탭과 같은 자리에서 연다. 우하단 하나로 모은다 */}
      <QuickAddLauncher
        knownTags={knownTags}
        variant="list"
        onCreate={handleCreate}
      />

      {editingId !== null && (
        <ScheduleEditModal
          id={editingId}
          knownTags={knownTags}
          variant="list"
          onClose={() => setEditingId(null)}
          onSaved={() => {
            // 3단을 넘기면 서버가 눌러 앉힌다. 목록을 다시 읽어야 결과가 맞는다
            void reload();
            void loadTags();
          }}
        />
      )}
    </div>
  );
}

/** 커서가 행의 위아래 어디쯤인지로 뜻을 읽는다 */
function intentAtPointer(e: DragEvent<HTMLElement>): DropIntent {
  const box = e.currentTarget.getBoundingClientRect();
  return dropIntentAt((e.clientY - box.top) / box.height);
}

interface RowProps {
  item: ScheduleListItem;
  depth: number;
  onToggle: () => void;
  onEdit: () => void;
  onDelete: () => void;
  /** 내 순서로 보는 중일 때만 끌 수 있다 */
  draggable: boolean;
  dragging: boolean;
  /** 지금 이 행에 놓으면 어떻게 되는지. 끌고 있지 않으면 null */
  dropIntent: DropIntent | null;
  onDragStart: () => void;
  onDragEnd: () => void;
  onDragOverRow: (intent: DropIntent) => void;
  onDropAt: (intent: DropIntent) => void;
}

function ListRow({
  item,
  depth,
  onToggle,
  onEdit,
  onDelete,
  draggable,
  dragging,
  dropIntent,
  onDragStart,
  onDragEnd,
  onDragOverRow,
  onDropAt,
}: RowProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const row = useRef<HTMLLIElement>(null);

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

  const done = Boolean(item.completedAt);
  // 안 하기로 한 일정. 지우지 않고 판단을 기록으로 남긴다
  const shelved = item.priority === "WONT";
  const badge = item.priority ? PRIORITY_BADGE[item.priority] : null;

  return (
    <li
      ref={row}
      // 우클릭도 같은 메뉴를 연다. 자리는 항상 같아야 다음에 어디를 볼지 안다
      onContextMenu={(e) => {
        e.preventDefault();
        setMenuOpen(true);
      }}
      onDragOver={(e) => {
        // 막지 않으면 브라우저가 놓기를 거부한다
        if (!draggable) return;
        e.preventDefault();
        onDragOverRow(intentAtPointer(e));
      }}
      onDrop={(e) => {
        e.preventDefault();
        onDropAt(intentAtPointer(e));
      }}
      className={`relative flex items-start gap-2 px-3 py-3 transition-opacity ${
        INDENT[depth] ?? ""
      } ${dragging ? "opacity-40" : ""} ${shelved ? "opacity-50" : ""} ${
        dropIntent ? DROP_MARK[dropIntent] : ""
      }`}
    >
      {/* 손잡이만 끈다. 행 전체를 끌면 글자를 고르는 것과 부딪힌다 */}
      <button
        type="button"
        draggable={draggable}
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
        disabled={!draggable}
        title={
          draggable
            ? "끌어서 순서 바꾸기. 행 가운데에 놓으면 하위로 들어갑니다"
            : "내 순서로 볼 때만 자리를 바꿀 수 있습니다"
        }
        aria-label={`${item.title} 순서 바꾸기`}
        className={`mt-0.5 shrink-0 px-1 ${
          draggable
            ? "cursor-grab text-slate-400 hover:text-slate-600 active:cursor-grabbing"
            : "cursor-not-allowed text-slate-200"
        }`}
      >
        ⠿
      </button>

      {/* 습관의 완료는 회차마다 있다. 리스트에는 회차가 없어 체크할 대상이 없다 */}
      {item.recurring ? (
        <span
          title="반복 일정입니다. 완료는 달력에서 회차마다 표시합니다"
          aria-label="반복 일정"
          className="mt-0.5 shrink-0 text-brand-400"
        >
          ⟳
        </span>
      ) : (
        <input
          type="checkbox"
          checked={done}
          onChange={onToggle}
          aria-label={`${item.title} 완료`}
          className="mt-1 size-4 shrink-0 accent-brand-500"
        />
      )}

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <button
          type="button"
          onClick={onEdit}
          className={`truncate text-left text-sm hover:underline ${
            done ? "text-slate-400 line-through" : "text-slate-800"
          }`}
        >
          {item.title}
        </button>

        <div className="flex flex-wrap items-center gap-1.5 text-xs text-slate-400">
          {badge && (
            <span className={`rounded-full px-2 py-0.5 ${badge.style}`}>
              {badge.label}
            </span>
          )}
          {item.dueOn && <span>기한 {item.dueOn}</span>}
          {item.startAt && <span>시작 {item.startAt.slice(0, 10)}</span>}
          {item.place && <span>{item.place}</span>}
          {item.tags?.map((tag) => (
            <span
              key={tag}
              className="rounded-full bg-brand-50 px-2 py-0.5 text-brand-700"
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
        className="shrink-0 rounded-md border border-transparent px-2 py-1 text-slate-400 transition-colors hover:border-slate-200 hover:bg-slate-50 hover:text-slate-700 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100"
      >
        ⋯
      </button>

      {menuOpen && (
        <div
          role="menu"
          className="absolute top-10 right-3 z-20 flex w-40 flex-col gap-0.5 rounded-xl border border-slate-200 bg-white p-1.5 shadow-lg"
        >
          <button
            type="button"
            role="menuitem"
            className={`${MENU_ITEM} text-slate-700 hover:bg-slate-100`}
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
            className={`${MENU_ITEM} text-red-600 hover:bg-red-50`}
            onClick={() => {
              setMenuOpen(false);
              onDelete();
            }}
          >
            삭제
          </button>
        </div>
      )}
    </li>
  );
}
