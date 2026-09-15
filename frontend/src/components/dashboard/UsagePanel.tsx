import { useState } from "react";
import { providerLabel } from "../../constants/aiPresets";
import type { AiUsageSummary, ProviderUsage } from "../../types/schedule";
import DashboardCard, { SegmentToggle } from "./DashboardCard";

type Period = "week" | "month";

interface Props {
  usage: AiUsageSummary;
}

/** 제공자별 AI 호출 수와 토큰. 이번 주와 이번 달을 오간다 */
export default function UsagePanel({ usage }: Props) {
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
            </li>
          ))}
        </ul>
      )}

      <p className="text-[11px] text-slate-400">석 달이 지난 기록은 지웁니다.</p>
    </DashboardCard>
  );
}
