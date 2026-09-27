import { useCallback, useEffect, useMemo, useState } from "react";
import {
  changeCompletion,
  changeInstanceStatus,
  createSchedule,
  deleteSchedule,
  fetchScheduleList,
  fetchTagNames,
  reorderSchedules,
} from "../api/schedules";
import QuickAddLauncher from "../components/quickadd/QuickAddLauncher";
import ScheduleEditModal from "../components/schedule/ScheduleEditModal";
import WarpBorder from "../components/layouts/WarpBorder";
import {
  planDrop,
  planDropAtEnd,
  type DropIntent,
  type DropPlan,
} from "../domain/scheduleDrop";
import { buildTree, flatten, type ListSort } from "../domain/scheduleTree";
import type {
  ScheduleCreateRequest,
  ScheduleListItem,
} from "../types/schedule";
import ListRow from "../components/layouts/List/ListRow";

const TOGGLE = "rounded-md border px-3 py-1 text-sm transition-colors";
const TOGGLE_ON = "border-accent bg-accent text-canvas";
const TOGGLE_OFF = "border-line text-ink-soft hover:bg-accent-soft";

const SORTS: { value: ListSort; label: string }[] = [
  { value: "manual", label: "내 순서" },
  { value: "due", label: "기한순" },
  { value: "priority", label: "우선순위순" },
];

export default function ListPage() {
  const [items, setItems] = useState<ScheduleListItem[]>([]);
  const [knownTags, setKnownTags] = useState<string[]>([]);
  const [sort, setSort] = useState<ListSort>("manual");
  const [editingId, setEditingId] = useState<number | null>(null);

  // 화살표로 미리 넘겨 본 회차. 저장은 건드리지 않는다
  const [peeked, setPeeked] = useState<Record<number, string>>({});
  const [draggingId, setDraggingId] = useState<number | null>(null);
  // 놓을 수 있을 때만 채운다. 화면은 세울 자리를 스스로 셈하지 않는다
  const [dropAt, setDropAt] = useState<{
    id: number;
    intent: DropIntent;
    plan: DropPlan;
  } | null>(null);

  // 마지막 행이 깊은 곳에 있으면 그 아래에는 최상위 자리가 없다
  const [dropAtEnd, setDropAtEnd] = useState(false);

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
  const handleCreate = async (
    body: ScheduleCreateRequest,
  ): Promise<boolean> => {
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

  const handleToggle = async (item: ScheduleListItem, onDate?: string) => {
    setError(null);
    try {
      // 반복의 완료는 일정이 아니라 회차가 갖는다
      if (item.recurring) {
        if (onDate === undefined) return;
        await changeInstanceStatus(item.id, onDate, "DONE");
      } else {
        await changeCompletion(item.id, !item.completedAt);
      }
      // 넘겨 보던 회차는 완료와 함께 의미를 잃는다
      setPeeked(({ [item.id]: _gone, ...rest }) => rest);
      await reload();
    } catch (e) {
      fail(e, "완료 상태를 바꾸지 못했습니다");
    }
  };

  const stopDragging = () => {
    setDraggingId(null);
    setDropAt(null);
    setDropAtEnd(false);
  };

  /** 끌어다 놓은 자리대로 무리를 다시 세운다 */
  const handleDrop = async (plan: DropPlan) => {
    stopDragging();
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
    if (
      !window.confirm(`"${item.title}" 을(를) 지울까요? 되돌릴 수 없습니다.`)
    ) {
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
        <h2 className="text-lg font-semibold text-ink">리스트</h2>

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

      <p className="text-xs text-ink-faint">
        날짜를 안 정한 일도 적어 둘 수 있습니다. 끌 때 행의 가운데에 놓으면 그
        일정의 막내 하위로, 위아래 틈에 놓으면 형제로 들어갑니다. 틈에서는
        좌우로 움직여 몇 단에 설지 고릅니다.
      </p>

      {error && (
        <p className="rounded-md border border-danger bg-danger-soft px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      <WarpBorder className="rounded-2xl bg-surface shadow-xs">
        {rows.length === 0 ? (
          <p className="px-5 py-8 text-center text-sm text-ink-faint">
            {loading ? "불러오는 중입니다…" : "아직 적어 둔 것이 없습니다."}
          </p>
        ) : (
          <ul className="divide-y divide-line">
            {rows.map(({ item, depth }) => (
              <ListRow
                key={item.id}
                item={item}
                depth={depth}
                shownOn={peeked[item.id] ?? item.occurrenceOn}
                onStep={(to) =>
                  setPeeked((seen) => ({ ...seen, [item.id]: to }))
                }
                onRewind={() =>
                  setPeeked(({ [item.id]: _gone, ...rest }) => rest)
                }
                onToggle={(onDate) => void handleToggle(item, onDate)}
                onEdit={() => setEditingId(item.id)}
                onDelete={() => void handleDelete(item)}
                draggable={canDrag}
                dragging={draggingId === item.id}
                drop={
                  dropAt?.id === item.id
                    ? { intent: dropAt.intent, level: dropAt.plan.level }
                    : null
                }
                onDragStart={() => setDraggingId(item.id)}
                onDragEnd={stopDragging}
                onDragOverRow={(intent, level) => {
                  const plan =
                    draggingId === null
                      ? null
                      : planDrop(items, draggingId, item.id, intent, level);

                  // 못 놓는 자리에는 아무 표시도 하지 않는다
                  setDropAt(plan && { id: item.id, intent, plan });
                  setDropAtEnd(false);
                }}
                onDropAt={() => {
                  if (dropAt?.id !== item.id) return;
                  void handleDrop(dropAt.plan);
                }}
              />
            ))}
          </ul>
        )}

        {/* 마지막 행에 붙이지 않고도 최상위 끝으로 뺄 수 있어야 한다 */}
        {draggingId !== null && (
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDropAt(null);
              setDropAtEnd(true);
            }}
            onDragLeave={() => setDropAtEnd(false)}
            onDrop={(e) => {
              e.preventDefault();
              if (draggingId === null) return;

              // 이미 끝자리면 옮길 것이 없다
              const plan = planDropAtEnd(items, draggingId);
              if (plan === null) stopDragging();
              else void handleDrop(plan);
            }}
            className={`m-2 rounded-xl border-2 border-dashed py-3 text-center text-xs transition-colors ${
              dropAtEnd
                ? "border-accent bg-accent-soft text-accent"
                : "border-line text-ink-faint"
            }`}
          >
            여기에 놓으면 맨 아래로 갑니다
          </div>
        )}
      </WarpBorder>

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
