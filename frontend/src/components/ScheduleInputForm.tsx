import { useEffect, useState } from "react";
import type { ScheduleCreateRequest } from "../types/schedule";
import { nextHour } from "../utils/datetime";

interface Props {
  /** 저장에 성공했는지 돌려준다. 실패하면 입력값을 지우지 않는다 */
  onSubmit: (body: ScheduleCreateRequest) => Promise<boolean>;
  knownCategories: string[];
  /** 달력에서 날짜를 고른 상태면 시작 시각을 그 날짜로 채워준다 */
  defaultDate?: Date | null;
  /** AI가 문장을 못 읽었을 때 친 문장을 제목으로 넘겨받는다 */
  initialTitle?: string;
  disabled?: boolean;
}

/** 입력칸 네 개가 같은 모양을 공유하므로 한곳에 모아둔다. */
const INPUT =
  "w-full rounded-md border border-slate-200 bg-white px-2.5 py-2 text-slate-800 placeholder:text-slate-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-3 focus:ring-brand-100";
const LABEL = "text-xs font-semibold tracking-wide text-slate-500";

export default function ScheduleInputForm({
  onSubmit,
  knownCategories,
  defaultDate,
  initialTitle,
  disabled,
}: Props) {
  const [title, setTitle] = useState(initialTitle ?? "");
  const [startAt, setStartAt] = useState(() => nextHour(defaultDate));
  const [endAt, setEndAt] = useState("");
  const [category, setCategory] = useState("");
  // 종료 시각은 안 쓰는 일정이 더 많아 기본으로 감춘다
  const [showEndAt, setShowEndAt] = useState(false);

  // 폼이 열릴 때와 고른 날짜가 바뀔 때 시작 시각을 다시 잡는다
  useEffect(() => {
    setStartAt(nextHour(defaultDate));
  }, [defaultDate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); // 폼 기본 동작(페이지 새로고침)을 막는다
    if (!title.trim() || !startAt) return;

    const created = await onSubmit({
      title: title.trim(),
      // datetime-local은 "2026-08-19T15:00" 형식이라 초를 붙여 백엔드 LocalDateTime에 맞춘다
      startAt: `${startAt}:00`,
      endAt: endAt ? `${endAt}:00` : undefined,
      category: category || undefined,
    });

    // 실패했는데 입력을 지우면 사용자가 처음부터 다시 써야 한다
    if (!created) return;

    setTitle("");
    setStartAt(nextHour(defaultDate));
    setEndAt("");
    setCategory("");
  };

  return (
    <form
      className="flex flex-col gap-3.5 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs"
      onSubmit={handleSubmit}
    >
      <div className="flex min-w-0 flex-col gap-1.5">
        <label className={LABEL} htmlFor="title">
          제목
        </label>
        <input
          id="title"
          className={INPUT}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="무엇을 할 계획인가요?"
          maxLength={200}
          autoFocus
          required
        />
      </div>

      <div className="flex flex-wrap gap-3">
        <div className="flex min-w-0 flex-1 basis-44 flex-col gap-1.5">
          <label className={LABEL} htmlFor="startAt">
            시작
          </label>
          <input
            id="startAt"
            className={INPUT}
            type="datetime-local"
            value={startAt}
            onChange={(e) => setStartAt(e.target.value)}
            required
          />
        </div>

        {showEndAt && (
          <div className="flex min-w-0 flex-1 basis-44 flex-col gap-1.5">
            <label className={LABEL} htmlFor="endAt">
              종료
            </label>
            <input
              id="endAt"
              className={INPUT}
              type="datetime-local"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
            />
          </div>
        )}

        <div className="flex min-w-0 flex-1 basis-44 flex-col gap-1.5">
          <label className={LABEL} htmlFor="category">
            분류 (선택)
          </label>
          {/* 자유 입력. 목록에 없는 이름도 쓸 수 있다 */}
          <input
            id="category"
            className={INPUT}
            list="category-options"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="예: 능력"
            maxLength={50}
          />
          <datalist id="category-options">
            {knownCategories.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-600 select-none">
          <input
            type="checkbox"
            className="size-4 cursor-pointer accent-brand-500"
            checked={showEndAt}
            onChange={(e) => {
              setShowEndAt(e.target.checked);
              // 칸을 닫으면 입력해 둔 값도 함께 버린다.
              // 보이지 않는 값이 저장되면 사용자가 예상하지 못한다
              if (!e.target.checked) setEndAt("");
            }}
          />
          종료 시각 입력
        </label>

        <button
          type="submit"
          className="rounded-md bg-brand-500 px-5 py-2 font-semibold text-white shadow-xs transition-colors hover:bg-brand-600 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-brand-100 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={disabled}
        >
          일정 추가
        </button>
      </div>
    </form>
  );
}
