import { useCallback, useEffect, useMemo, useState } from "react";
import {
  changeCompletion,
  createSchedule,
  fetchScheduleList,
  fetchTagNames,
} from "../api/schedules";
import ScheduleInputForm from "../components/ScheduleInputForm";
import type { ParentOption } from "../components/schedule/ScheduleFields";
import { buildTree, flatten, type ListSort } from "../domain/scheduleTree";
import type { ScheduleCreateRequest, ScheduleListItem } from "../types/schedule";

/** 깊이만큼 들여쓴다. 계층은 3단까지라 세 칸이면 된다 */
const INDENT = ["", "pl-6", "pl-12"];

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

  // 3단이 꽉 찬 자리는 후보에서 뺀다. 서버가 눌러 앉히기 전에 못 고르게 한다
  const parentOptions = useMemo<ParentOption[]>(
    () =>
      flatten(buildTree(items))
        .filter(({ depth }) => depth < INDENT.length - 1)
        .map(({ item, depth }) => ({ id: item.id, title: item.title, depth })),
    [items],
  );

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
              />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

interface RowProps {
  item: ScheduleListItem;
  depth: number;
  onToggle: () => void;
}

function ListRow({ item, depth, onToggle }: RowProps) {
  const done = Boolean(item.completedAt);

  return (
    <li className={`flex items-start gap-2.5 px-5 py-3 ${INDENT[depth] ?? ""}`}>
      <input
        type="checkbox"
        checked={done}
        onChange={onToggle}
        aria-label={`${item.title} 완료`}
        className="mt-0.5 size-4 shrink-0 accent-brand-500"
      />

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <span
          className={`truncate text-sm ${
            done ? "text-slate-400 line-through" : "text-slate-800"
          }`}
        >
          {item.title}
        </span>

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
    </li>
  );
}
