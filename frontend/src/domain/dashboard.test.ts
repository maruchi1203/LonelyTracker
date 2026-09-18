import { describe, expect, it } from "vitest";
import { habitSummary, limitRatio, todayAgenda, weekOf, weekStats } from "./dashboard";
import type { Habit } from "../types/habit";
import type {
  AiProvider,
  ProviderUsage,
  ScheduleListItem,
  ScheduleResponse,
} from "../types/schedule";

const TODAY = "2026-09-15"; // 화요일

function instance(overrides: Partial<ScheduleResponse> = {}): ScheduleResponse {
  return {
    id: 1,
    title: "일정",
    allDay: false,
    recurring: false,
    status: "PLANNED",
    createdAt: "2026-09-01T00:00:00",
    updatedAt: "2026-09-01T00:00:00",
    ...overrides,
  };
}

function item(overrides: Partial<ScheduleListItem> = {}): ScheduleListItem {
  return {
    id: 100,
    displayOrder: 0,
    title: "항목",
    recurring: false,
    createdAt: "2026-09-01T00:00:00",
    updatedAt: "2026-09-01T00:00:00",
    ...overrides,
  };
}

function habit(overrides: Partial<Habit> = {}): Habit {
  return {
    id: 1,
    title: "명상",
    category: "MIND",
    displayOrder: 0,
    archived: false,
    doneDates: [],
    createdAt: "2026-09-01T00:00:00",
    updatedAt: "2026-09-01T00:00:00",
    ...overrides,
  };
}

describe("weekOf", () => {
  it("화요일이면 그 주 월요일부터 일요일까지다", () => {
    expect(weekOf(TODAY)).toEqual({ from: "2026-09-14", to: "2026-09-20" });
  });

  it("일요일은 앞선 월요일의 주에 속한다", () => {
    expect(weekOf("2026-09-20")).toEqual({ from: "2026-09-14", to: "2026-09-20" });
  });

  it("달을 넘겨도 이어진다", () => {
    expect(weekOf("2026-10-01")).toEqual({ from: "2026-09-28", to: "2026-10-04" });
  });
});

describe("todayAgenda", () => {
  it("종일이 앞에 오고 나머지는 시작 시각 순이다", () => {
    const agenda = todayAgenda(
      [
        instance({ id: 1, startAt: `${TODAY}T15:00:00` }),
        instance({ id: 2, startAt: `${TODAY}T09:00:00` }),
        instance({ id: 3, startAt: `${TODAY}T00:00:00`, allDay: true }),
      ],
      [],
      TODAY,
    );

    expect(agenda.scheduled.map((s) => s.id)).toEqual([3, 2, 1]);
  });

  it("오늘에 걸친 기간 일정도 오늘에 올라온다", () => {
    const agenda = todayAgenda(
      [instance({ startAt: "2026-09-14T09:00:00", endAt: "2026-09-16T18:00:00" })],
      [],
      TODAY,
    );

    expect(agenda.scheduled).toHaveLength(1);
  });

  it("자정에 끝나는 기간은 그날로 넘어오지 않는다", () => {
    const agenda = todayAgenda(
      [instance({ startAt: "2026-09-14T22:00:00", endAt: `${TODAY}T00:00:00` })],
      [],
      TODAY,
    );

    expect(agenda.scheduled).toHaveLength(0);
  });

  it("날짜 없는 항목은 회차로 올리지 않는다", () => {
    const agenda = todayAgenda([instance({ startAt: undefined, dueOn: TODAY })], [], TODAY);

    expect(agenda.scheduled).toHaveLength(0);
  });

  it("지난 마감은 끝내지 않은 것만 오래된 순으로 모은다", () => {
    const agenda = todayAgenda(
      [],
      [
        item({ id: 1, dueOn: "2026-09-13" }),
        item({ id: 2, dueOn: "2026-09-10" }),
        item({ id: 3, dueOn: "2026-09-11", completedAt: "2026-09-11T10:00:00" }),
        item({ id: 4, dueOn: "2026-09-12", priority: "WONT" }),
        item({ id: 5, dueOn: TODAY }),
      ],
      TODAY,
    );

    expect(agenda.overdue.map((i) => i.id)).toEqual([2, 1]);
  });

  it("오늘 회차에 이미 있는 일정은 오늘 마감에 다시 세우지 않는다", () => {
    const agenda = todayAgenda(
      [instance({ id: 7, startAt: `${TODAY}T10:00:00` })],
      [item({ id: 7, dueOn: TODAY }), item({ id: 8, dueOn: TODAY })],
      TODAY,
    );

    expect(agenda.dueToday.map((i) => i.id)).toEqual([8]);
  });
});

describe("weekStats", () => {
  it("끝내지 않은 필수만 모으고 반복은 뺀다", () => {
    const stats = weekStats(
      [
        item({ id: 1, priority: "MUST" }),
        item({ id: 2, priority: "MUST", completedAt: "2026-09-14T10:00:00" }),
        item({ id: 3, priority: "SHOULD" }),
        item({ id: 4, priority: "MUST", recurring: true }),
      ],
      TODAY,
    );

    expect(stats.must.map((i) => i.id)).toEqual([1]);
  });

  it("권장은 따로 모으고 우선순위 없는 것은 넣지 않는다", () => {
    const stats = weekStats(
      [
        item({ id: 1, priority: "SHOULD" }),
        item({ id: 2 }),
        item({ id: 3, priority: "COULD" }),
      ],
      TODAY,
    );

    expect(stats.should.map((i) => i.id)).toEqual([1]);
  });

  it("마감이 가까운 것이 앞이고 마감 없는 것은 뒤다", () => {
    const stats = weekStats(
      [
        item({ id: 1, priority: "MUST" }),
        item({ id: 2, priority: "MUST", dueOn: "2026-09-20" }),
        item({ id: 3, priority: "MUST", dueOn: "2026-09-16" }),
      ],
      TODAY,
    );

    expect(stats.must.map((i) => i.id)).toEqual([3, 2, 1]);
  });

  it("마감 임박은 오늘부터 7일 안에서 가까운 순이다", () => {
    const stats = weekStats(
      [
        item({ id: 1, dueOn: "2026-09-21" }),
        item({ id: 2, dueOn: TODAY }),
        item({ id: 3, dueOn: "2026-09-22" }),
        item({ id: 4, dueOn: "2026-09-14" }),
      ],
      TODAY,
    );

    expect(stats.dueSoon.map((i) => i.id)).toEqual([2, 1]);
  });
});

describe("habitSummary", () => {
  it("오늘 한 것과 연속일 상위, 빈 갈래를 모은다", () => {
    const summary = habitSummary(
      [
        habit({ id: 1, category: "BODY", doneDates: ["2026-09-13", "2026-09-14", TODAY] }),
        habit({ id: 2, category: "MIND", doneDates: ["2026-09-14"] }),
        habit({ id: 3, category: "ART", doneDates: [] }),
        habit({ id: 4, category: "LEARNING", archived: true, doneDates: [TODAY] }),
      ],
      TODAY,
    );

    expect(summary.doneToday).toBe(1);
    expect(summary.total).toBe(3);
    expect(summary.topStreaks.map((s) => [s.habit.id, s.streak])).toEqual([
      [1, 3],
      [2, 1],
    ]);
    expect(summary.emptyCategories).toEqual(["부업", "학습", "인간관계"]);
  });
});

describe("limitRatio", () => {
  const usage: ProviderUsage = {
    baseUrl: "https://api.openai.com/v1",
    calls: 3,
    inputTokens: 300,
    outputTokens: 100,
  };

  function provider(overrides: Partial<AiProvider> = {}): AiProvider {
    return {
      id: 1,
      baseUrl: "https://api.openai.com/v1",
      model: "m",
      masked: "****abcd",
      active: true,
      ...overrides,
    };
  }

  it("한도를 정하지 않은 제공자면 막대가 없다", () => {
    expect(limitRatio(usage, [provider()])).toBeNull();
  });

  it("목록에 없는 제공자면 막대가 없다", () => {
    expect(limitRatio(usage, [])).toBeNull();
  });

  it("주소 표기가 달라도 같은 제공자로 본다", () => {
    const written = provider({
      baseUrl: "HTTPS://API.OPENAI.COM/v1/",
      monthlyTokenLimit: 1000,
    });

    expect(limitRatio(usage, [written])).toBe(0.4);
  });

  it("한도를 넘겨도 1을 넘지 않는다", () => {
    expect(limitRatio(usage, [provider({ monthlyTokenLimit: 100 })])).toBe(1);
  });
});
