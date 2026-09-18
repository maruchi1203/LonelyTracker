import { useState } from "react";
import { providerLabel } from "../../constants/aiPresets";
import { limitRatio } from "../../domain/dashboard";
import type {
  AiProvider,
  AiUsageSummary,
  ProviderUsage,
} from "../../types/schedule";
import DashboardCard, { SegmentToggle } from "./DashboardCard";

type Period = "week" | "month";

interface Props {
  usage: AiUsageSummary;
  providers: AiProvider[];
}

/** 제공자별 AI 호출 수와 토큰. 이번 주와 이번 달을 오간다 */
export default function UsagePanel({ usage, providers }: Props) {
  const [period, setPeriod] = useState<Period>("week");
  const rows: ProviderUsage[] = usage[period];

  return (
    <DashboardCard
      title="AI 사용량"
      action={
        <SegmentToggle
          options={[
            { value: "week", label: "이번 주" },
            { value: "month", label: "이번 달" },
          ]}
          value={period}
          onChange={setPeriod}
        />
      }
    >
      {rows.length === 0 ? (
        <p className="py-6 text-center text-sm text-slate-400">
          {period === "week" ? "이번 주" : "이번 달"}에는 AI 를 부르지 않았습니다.
        </p>
      ) : (
        <ul className="flex list-none flex-col gap-2 p-0">
          {rows.map((r) => (
            <li key={r.baseUrl} className="flex flex-col gap-0.5">
              <div className="flex items-baseline justify-between gap-2 text-sm">
                <span className="min-w-0 truncate font-medium text-slate-700">
                  {providerLabel(r.baseUrl)}
                </span>
                <span className="shrink-0 text-slate-800">
                  <span className="font-semibold">{r.calls.toLocaleString()}</span>
                  <span className="ml-0.5 text-xs text-slate-400">회</span>
                </span>
              </div>
              <div className="flex justify-between gap-2 text-xs text-slate-400">
                <span>토큰 {(r.inputTokens + r.outputTokens).toLocaleString()}</span>
                <span>
                  입력 {r.inputTokens.toLocaleString()} · 출력 {r.outputTokens.toLocaleString()}
                </span>
              </div>

              <LimitBar usage={r} providers={providers} period={period} />
            </li>
          ))}
        </ul>
      )}

      <p className="text-[11px] text-slate-400">석 달이 지난 기록은 지웁니다.</p>
    </DashboardCard>
  );
}

/** 이번 달 한도를 정해 둔 제공자에만 보이는 막대. 8할을 넘으면 색이 바뀐다 */
function LimitBar({
  usage,
  providers,
  period,
}: {
  usage: ProviderUsage;
  providers: AiProvider[];
  period: Period;
}) {
  // 한도는 달 단위라 이번 주 보기에서는 견줄 대상이 아니다
  const ratio = period === "month" ? limitRatio(usage, providers) : null;
  if (ratio === null) return null;

  const near = ratio >= 0.8;

  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-slate-100">
        <div
          className={`h-full rounded-full ${near ? "bg-amber-500" : "bg-brand-500"}`}
          style={{ width: `${Math.round(ratio * 100)}%` }}
        />
      </div>
      <span className={`shrink-0 text-[11px] ${near ? "text-amber-600" : "text-slate-400"}`}>
        한도의 {Math.round(ratio * 100)}%
      </span>
    </div>
  );
}
