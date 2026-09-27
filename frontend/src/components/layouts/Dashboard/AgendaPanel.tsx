import { useState } from "react";
import DashboardPanel, { SegmentToggle } from "./DashboardPanel";
import TodayView, { type AgendaProps } from "./TodayView";
import WeekView from "./WeekView";

type View = "today" | "week";

/** 오늘 할 일과 이번 주 요약을 한 칸에서 오간다 */
export default function ScheduleSummaryPanel(props: AgendaProps) {
  const [view, setView] = useState<View>("today");

  return (
    <DashboardPanel
      title={view === "today" ? "오늘" : "이번 주"}
      action={
        <SegmentToggle
          options={[
            { value: "today", label: "Today" },
            { value: "week", label: "Week" },
          ]}
          value={view}
          onChange={setView}
        />
      }
    >
      {view === "today" ? (
        <TodayView {...props} />
      ) : (
        <WeekView items={props.items} today={props.today} />
      )}
    </DashboardPanel>
  );
}
