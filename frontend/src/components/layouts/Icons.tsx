import type { ReactNode, SVGProps } from "react";

/**
 * 선으로 그린 아이콘들. 굵기와 크기를 여기 한 자리에서 맞춘다.
 *
 * 아이콘 라이브러리를 넣지 않는 이유는 쓰는 개수가 한 줌이어서다.
 * 열 개를 넘어가면 그때 라이브러리를 들인다
 */
function Glyph({
  children,
  ...rest
}: { children: ReactNode } & SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className="size-5 shrink-0"
      {...rest}
    >
      {children}
    </svg>
  );
}

/** AI 에게 읽힌다 */
export function SparkIcon() {
  return (
    <Glyph>
      <path d="M12 3.5 13.7 9l5.5 1.7-5.5 1.7L12 18l-1.7-5.6L4.8 10.7 10.3 9z" />
      <path d="M18.5 3.5v3M20 5h-3" />
    </Glyph>
  );
}

/** 하던 것을 그만둔다 */
export function StopIcon() {
  return (
    <Glyph>
      <rect x="7" y="7" width="10" height="10" rx="2" />
    </Glyph>
  );
}

/** 손으로 직접 적는다 */
export function PencilIcon() {
  return (
    <Glyph>
      <path d="M4 20h4L19.2 8.8a2.6 2.6 0 0 0-3.7-3.7L4.3 16.3z" />
      <path d="M14.8 6.3 17.9 9.4" />
    </Glyph>
  );
}

export function CloseIcon() {
  return (
    <Glyph>
      <path d="M6.5 6.5 17.5 17.5M17.5 6.5 6.5 17.5" />
    </Glyph>
  );
}

export function PlusIcon() {
  return (
    <Glyph>
      <path d="M12 5.5v13M5.5 12h13" />
    </Glyph>
  );
}

/* ── 일정 종류 ────────────────────────────────── */

/** 간단 — 시간 위의 한 점 */
export function MomentIcon() {
  return (
    <Glyph>
      <path d="M4 18h16" />
      <circle cx="12" cy="10" r="3.2" />
    </Glyph>
  );
}

/** 기간 — 시작과 끝을 잇는 구간 */
export function RangeIcon() {
  return (
    <Glyph>
      <path d="M5 12h14M5 8v8M19 8v8" />
    </Glyph>
  );
}

/** 반복 — 되돌아오는 고리 */
export function RepeatIcon() {
  return (
    <Glyph>
      <path d="M4.5 10.5A7.5 7.5 0 0 1 18 7.4" />
      <path d="M18 3.5v4h-4" />
      <path d="M19.5 13.5A7.5 7.5 0 0 1 6 16.6" />
      <path d="M6 20.5v-4h4" />
    </Glyph>
  );
}

/* ── 우선순위 ─────────────────────────────────── */
/* 위로 향할수록 급하고, 가로줄이 보통, 그어진 동그라미가 안 하기로 한 것이다 */

/** 필수 */
export function MustIcon() {
  return (
    <Glyph>
      <path d="M6 12.5 12 7l6 5.5M6 18l6-5.5 6 5.5" />
    </Glyph>
  );
}

/** 권장 */
export function ShouldIcon() {
  return (
    <Glyph>
      <path d="M6 15.5 12 9l6 6.5" />
    </Glyph>
  );
}

/** 선택 */
export function CouldIcon() {
  return (
    <Glyph>
      <path d="M6 12h12" />
    </Glyph>
  );
}

/** 보류 */
export function WontIcon() {
  return (
    <Glyph>
      <circle cx="12" cy="12" r="7.5" />
      <path d="M6.7 17.3 17.3 6.7" />
    </Glyph>
  );
}

/* ── 반복 주기 ────────────────────────────────── */
/* 같은 달력 틀 안에 든 것이 늘어날수록 성긴 주기다 */

/** 매일 */
export function DailyIcon() {
  return (
    <Glyph>
      <CalendarFrame />
      <path d="M12 14h.01" />
    </Glyph>
  );
}

/** 매주 */
export function WeeklyIcon() {
  return (
    <Glyph>
      <CalendarFrame />
      <path d="M7.5 14h9" />
    </Glyph>
  );
}

/** 매월 */
export function MonthlyIcon() {
  return (
    <Glyph>
      <CalendarFrame />
      <path d="M7.5 13h9M7.5 16.5h9" />
    </Glyph>
  );
}

/** 세 주기가 함께 쓰는 달력 테두리. 안쪽 내용만 달라진다 */
function CalendarFrame() {
  return (
    <>
      <rect x="3.5" y="5" width="17" height="15.5" rx="2.5" />
      <path d="M3.5 10h17M8 3.5v3M16 3.5v3" />
    </>
  );
}

/** 시각을 더한다 */
export function ClockIcon() {
  return (
    <Glyph>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 7.5V12l3 2" />
    </Glyph>
  );
}

/* ── 회차 상태 ────────────────────────────────── */

/** 예정 — 아직 손대지 않은 것 */
export function PlannedIcon() {
  return (
    <Glyph>
      <circle cx="12" cy="12" r="7.5" />
    </Glyph>
  );
}

/** 완료 */
export function DoneIcon() {
  return (
    <Glyph>
      <circle cx="12" cy="12" r="7.5" />
      <path d="m8.5 12 2.5 2.5 4.5-5" />
    </Glyph>
  );
}

/** 건너뜀 — 안 한 것을 안 했다고 남긴다 */
export function SkipIcon() {
  return (
    <Glyph>
      <circle cx="12" cy="12" r="7.5" />
      <path d="M9 12h6" />
    </Glyph>
  );
}
