import { weekOf, weekStats } from "../../../domain/dashboard";
import type { ScheduleListItem } from "../../../types/schedule";
import { shortDate } from "../../../utils/datetime";

/** 이번 주 보기에서 항목마다 보여줄 줄 수 */
const WEEK_ROWS = 5;


export default function WeekView({
  items,
  today,
}: {
  items: ScheduleListItem[];
  today: string;
}) {
  const { must, should, dueSoon } = weekStats(items, today);
  const week = weekOf(today);

  return (
    <div className="flex flex-col gap-4">
      <p className="text-xs text-ink-faint">
        {shortDate(week.from)} ~ {shortDate(week.to)}
      </p>
      <ItemSection
        title="필수"
        empty="없음"
        items={must}
        tone="text-danger"
      />
      <ItemSection
        title="권장"
        empty="없음"
        items={should}
        tone="text-warn"
      />
      <ItemSection
        title="마감"
        empty="없음"
        items={dueSoon}
        tone="text-ink"
      />
    </div>
  );
}

interface SectionProps {
  title: string;
  empty: string;
  items: ScheduleListItem[];
  /** 개수 숫자의 색 */
  tone: string;
}

function ItemSection({ title, empty, items, tone }: SectionProps) {
  const rest = items.length - WEEK_ROWS;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between text-sm">
        <span className="text-ink-soft">{title}</span>
        <span
          className={`font-semibold ${items.length > 0 ? tone : "text-ink-faint"}`}
        >
          {items.length}
        </span>
      </div>

      {items.length === 0 ? (
        <p className="text-xs text-ink-faint">{empty}</p>
      ) : (
        <ul className="flex list-none flex-col gap-0.5 p-0">
          {items.slice(0, WEEK_ROWS).map((i) => (
            <li key={i.id} className="flex items-center gap-2 text-sm">
              <span className="w-11 shrink-0 text-xs text-ink-faint">
                {i.dueOn ? shortDate(i.dueOn) : "—"}
              </span>
              <span className="min-w-0 flex-1 truncate text-ink">
                {i.title}
              </span>
            </li>
          ))}
          {rest > 0 && <li className="text-xs text-ink-faint">외 {rest}개</li>}
        </ul>
      )}
    </div>
  );
}
