import type { ReactNode } from "react";

interface Props {
  title: string;
  /** 제목 오른쪽에 두는 버튼이나 링크 */
  action?: ReactNode;
  children: ReactNode;
}

/** 대시보드 칸 하나의 틀 */
export default function DashboardCard({ title, action, children }: Props) {
  return (
    <section className="flex min-w-0 flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-slate-800">{title}</h3>
        {action}
      </div>
      {children}
    </section>
  );
}

/** 아직 데이터가 없는 칸. 가짜 숫자를 넣지 않는다 */
export function PendingCard({ title, summary }: { title: string; summary: string }) {
  return (
    <DashboardCard title={title}>
      <p className="rounded-xl border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-400">
        {summary}
      </p>
    </DashboardCard>
  );
}

interface ToggleProps<T extends string> {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
}

/** 한 칸 안에서 기간이나 보기를 오가는 버튼 묶음 */
export function SegmentToggle<T extends string>({ options, value, onChange }: ToggleProps<T>) {
  return (
    <div role="tablist" className="flex rounded-md border border-slate-200 p-0.5 text-xs">
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="tab"
          aria-selected={value === o.value}
          onClick={() => onChange(o.value)}
          className={`rounded px-2.5 py-1 transition-colors ${
            value === o.value
              ? "bg-brand-500 font-semibold text-white"
              : "text-slate-500 hover:bg-slate-50"
          }`}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
