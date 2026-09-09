import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  changeCompletion,
  createSchedule,
  deleteSchedule,
  fetchScheduleList,
  fetchTagNames,
} from "../api/schedules";
import ScheduleInputForm from "../components/ScheduleInputForm";
import ScheduleEditModal from "../components/schedule/ScheduleEditModal";
import type { ParentOption } from "../components/schedule/ScheduleFields";
import {
  buildTree,
  flatten,
  selfAndDescendantIds,
  type ListSort,
} from "../domain/scheduleTree";
import type { ScheduleCreateRequest, ScheduleListItem } from "../types/schedule";

/** 깊이만큼 들여쓴다. 계층은 3단까지라 세 칸이면 된다 */
const INDENT = ["", "pl-6", "pl-12"];

const MENU_ITEM =
  "rounded-md px-2.5 py-1.5 text-left text-sm transition-colors";

const TOGGLE = "rounded-md border px-3 py-1 text-sm transition-colors";
const TOGGLE_ON = "border-brand-500 bg-brand-500 text-white";
const TOGGLE_OFF = "border-slate-200 text-slate-600 hover:bg-brand-50";

const SORTS: { value: ListSort; label: string }[] = [
  { value: "manual", label: "내 순서" },
  { value: "due", label: "기한순" },
];

export default function ListPage() {
  const [items, setItems] = useState<ScheduleListItem[]>([]);
  const [knownTags, setKnownTags] = useState<string[]>([]);
  const [sort, setSort] = useState<ListSort>("manual");
  const [editingId, setEditingId] = useState<number | null>(null);
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

  /**
   * 상위로 고를 수 있는 목록
   * 3단이 꽉 찬 자리는 뺀다. 서버가 눌러 앉히기 전에 못 고르게 한다
   *
   * @param excludeId 수정 중인 항목. 자기 자신과 자기 자손은 부모가 될 수 없다
   */
  const optionsFor = useCallback(
    (excludeId?: number): ParentOption[] => {
      const blocked =
        excludeId === undefined
          ? new Set<number>()
          : selfAndDescendantIds(items, excludeId);

      return flatten(buildTree(items))
        .filter(
          ({ item, depth }) =>
            depth < INDENT.length - 1 && !blocked.has(item.id),
        )
        .map(({ item, depth }) => ({ id: item.id, title: item.title, depth }));
    },
    [items],
  );

  const parentOptions = useMemo(() => optionsFor(), [optionsFor]);

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
        날짜를 안 정한 일도 적어 둘 수 있습니다. 습관은 습관일지 탭에 있습니다.
      </p>

      {error && (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </p>
      )}

      <ScheduleInputForm
        onSubmit={handleCreate}
        knownTags={knownTags}
        variant="list"
        parentOptions={parentOptions}
        disabled={loading}
      />

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
              />
            ))}
          </ul>
        )}
      </section>

      {editingId !== null && (
        <ScheduleEditModal
          id={editingId}
          knownTags={knownTags}
          parentOptions={optionsFor(editingId)}
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

interface RowProps {
  item: ScheduleListItem;
  depth: number;
  onToggle: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

function ListRow({ item, depth, onToggle, onEdit, onDelete }: RowProps) {
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

  return (
    <li
      ref={row}
      // 우클릭도 같은 메뉴를 연다. 자리는 항상 같아야 다음에 어디를 볼지 안다
      onContextMenu={(e) => {
        e.preventDefault();
        setMenuOpen(true);
      }}
      className={`relative flex items-start gap-2 px-3 py-3 ${INDENT[depth] ?? ""}`}
    >
      {/* 순서 바꾸기 자리. 재정렬 API 가 아직 없어 잡아만 둔다 */}
      <button
        type="button"
        disabled
        title="순서 바꾸기는 준비 중입니다"
        aria-label={`${item.title} 순서 바꾸기 (준비 중)`}
        className="mt-0.5 shrink-0 cursor-not-allowed px-1 text-slate-300"
      >
        ⠿
      </button>

      <input
        type="checkbox"
        checked={done}
        onChange={onToggle}
        aria-label={`${item.title} 완료`}
        className="mt-1 size-4 shrink-0 accent-brand-500"
      />

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
