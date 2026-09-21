import { NavLink } from "react-router";

const ITEM =
  "flex items-center justify-between gap-2 rounded-md px-2.5 py-1.5 text-sm transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line";
const ITEM_ON = "bg-accent font-medium text-canvas";
const ITEM_OFF = "text-ink-soft hover:bg-surface hover:text-ink";

interface SideMenuItem {
  to: string;
  label: string;
  /** 화면만 있고 아직 데이터가 없는 메뉴 */
  comingSoon?: boolean;
}

const MENU: SideMenuItem[] = [
  { to: "/dashboard", label: "대시보드" },
  { to: "/list", label: "리스트" },
  { to: "/calendar", label: "달력" },
  { to: "/projects", label: "프로젝트", comingSoon: true },
  { to: "/habits", label: "습관일지" },
  { to: "/settings", label: "설정" },
];

export default function SideMenu() {
  return (
    <nav
      aria-label="주요 메뉴"
      className="flex w-52 shrink-0 flex-col gap-1 border-r border-line pr-4"
    >
      {MENU.map(({ to, label, comingSoon }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            `${ITEM} ${isActive ? ITEM_ON : ITEM_OFF}`
          }
        >
          {({ isActive }) => (
            <>
              <span className="truncate">{label}</span>
              {comingSoon && (
                <span
                  className={`shrink-0 rounded-full px-1.5 py-0.5 text-[10px] ${
                    isActive
                      ? "bg-canvas/20 text-canvas"
                      : "bg-surface-soft text-ink-faint"
                  }`}
                >
                  WIP
                </span>
              )}
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}
