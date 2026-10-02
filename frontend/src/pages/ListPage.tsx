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
import { useQuickAddTarget } from "../components/quickadd/QuickAddContext";
import ScheduleEditModal from "../components/schedule/ScheduleEditModal";
import {
  planDrop,
  planDropAtEnd,
  type DropIntent,
  type DropPlan,
} from "../domain/scheduleDrop";
import {
  buildTree,
  isOccurrenceDone,
  type ListSort,
  type TreeNode,
} from "../domain/scheduleTree";
import type {
  ScheduleCreateRequest,
  ScheduleListItem,
} from "../types/schedule";
import ListRow from "../components/layouts/List/ListRow";
import WarpBorder from "../components/layouts/WarpBorder";

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

  const tree = useMemo(() => buildTree(items, sort), [items, sort]);

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

  // 리스트는 날짜를 요구하지 않는다
  useQuickAddTarget(
    { defaultDate: null, knownTags, variant: "list" },
    handleCreate,
  );

  const handleToggle = async (item: ScheduleListItem, onDate?: string) => {
    setError(null);
    try {
      // 반복의 완료는 일정이 아니라 회차가 갖는다
      if (item.recurring) {
        if (onDate === undefined) return;
        // 끝낸 회차를 다시 누르면 되돌린다. 늘 DONE 을 보내면 해제할 길이 없다
        await changeInstanceStatus(
          item.id,
          onDate,
          isOccurrenceDone(item, onDate) ? "PLANNED" : "DONE",
        );
      } else {
        await changeCompletion(item.id, !item.completedAt);
      }
      // 지금 할 회차를 끝냈으면 다음 회차가 올라온다. 넘겨 보던 자리는 뜻을 잃는다.
      // 지난 회차를 손본 것이면 보던 자리를 그대로 둔다
      if (onDate === undefined || onDate === item.occurrenceOn) {
        setPeeked(({ [item.id]: _gone, ...rest }) => rest);
      }
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

  /** 하위 행은 부모의 액자 안으로 들어간다. 그래서 평평하게 펴지 않는다 */
  const renderNode = (node: TreeNode, depth = 0) => {
    const { item } = node;

    return (
      <ListRow
        key={item.id}
        item={item}
        depth={depth}
        shownOn={peeked[item.id] ?? item.occurrenceOn}
        onStep={(to) => setPeeked((seen) => ({ ...seen, [item.id]: to }))}
        onRewind={() => setPeeked(({ [item.id]: _gone, ...rest }) => rest)}
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
      >
        {node.children.map((child) => renderNode(child, depth + 1))}
      </ListRow>
    );
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

      {/*
        일렁이는 액자는 목록 전체에 하나만 둔다. 행마다 두면 필터가 행 수만큼
        돌고, 줄줄이 흔들려 글자를 읽기 어렵다. 달력이 격자를 감싸는 것과 같은 모양이다
      */}
      <WarpBorder className="rounded-2xl p-5">
        {tree.length === 0 ? (
          <p className="px-5 py-8 text-center text-sm text-ink-faint">
            {loading ? "불러오는 중입니다…" : "아직 적어 둔 것이 없습니다."}
          </p>
        ) : (
          <ul data-list-root className="flex flex-col gap-y-2">
            {tree.map((node) => renderNode(node))}
          </ul>
        )}
      </WarpBorder>

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
