import { useState } from "react";
import { providerLabel } from "../../constants/aiPresets";
import { limitRatio } from "../../domain/dashboard";
import type {
  AiProvider,
  AiUsageSummary,
  ProviderUsage,
} from "../../types/schedule";
import DashboardPanel, { SegmentToggle } from "./DashboardPanel";

type Period = "week" | "month";

interface Props {
  usage: AiUsageSummary;
  providers: AiProvider[];
}

/** 제공자별 AI 호출 수와 토큰. 이번 주와 이번 달을 오간다 */
export default function AiUsagePanel({ usage, providers }: Props) {
  const [period, setPeriod] = useState<Period>("week");
  const rows: ProviderUsage[] = usage[period];

  return (
    <DashboardPanel
      title="AI 사용량"
      action={
        <SegmentToggle
          options={[
            { value: "week", label: "Week" },
            { value: "month", label: "Month" },
          ]}
          value={period}
          onChange={setPeriod}
        />
      }
    >
      {rows.length === 0 ? (
        <p className="py-6 text-center text-sm text-ink-faint">
          {period === "week" ? "이번 주" : "이번 달"}에는 AI 호출이 없네요
        </p>
      ) : (
        <ul className="flex list-none flex-col gap-2 p-0">
          {rows.map((r) => (
            <li key={r.baseUrl} className="flex flex-col gap-0.5">
              <div className="flex items-baseline justify-between gap-2 text-sm">
                <span className="min-w-0 truncate font-medium text-ink">
                  {providerLabel(r.baseUrl)}
                </span>
                <span className="shrink-0 text-ink">
                  <span className="font-semibold">
                    {r.calls.toLocaleString()}
                  </span>
                  <span className="ml-0.5 text-xs text-ink-faint">회</span>
                </span>
              </div>
              <div className="flex justify-between gap-2 text-xs text-ink-faint">
                <span>
                  토큰 {(r.inputTokens + r.outputTokens).toLocaleString()}
                </span>
                <span>
                  입력 {r.inputTokens.toLocaleString()} · 출력{" "}
                  {r.outputTokens.toLocaleString()}
                </span>
              </div>

              <LimitBar usage={r} providers={providers} period={period} />
            </li>
          ))}
        </ul>
      )}

      <p className="text-[11px] text-ink-faint">
        3개월이 지난 기록은 삭제합니다
      </p>
    </DashboardPanel>
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
      <div className="h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-surface-soft">
        <div
          className={`h-full rounded-full ${near ? "bg-warn" : "bg-accent"}`}
          style={{ width: `${Math.round(ratio * 100)}%` }}
        />
      </div>
      <span
        className={`shrink-0 text-[11px] ${near ? "text-warn" : "text-ink-faint"}`}
      >
        한도의 {Math.round(ratio * 100)}%
      </span>
    </div>
  );
}
