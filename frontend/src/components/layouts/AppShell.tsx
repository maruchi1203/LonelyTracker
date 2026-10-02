import { Outlet } from "react-router";
import Header from "./Header";
import SideMenu from "./SideMenu";
import QuickAddLauncher from "../quickadd/QuickAddLauncher";
import { QuickAddProvider } from "../quickadd/QuickAddContext";

/** 화면 뼈대. 헤더 + 좌측 메뉴 + 본문 배치만 담당한다 */
export default function AppShell() {
  return (
    <QuickAddProvider>
      <div className="mx-auto max-w-6xl px-5 pt-8 pb-20">
        <Header />

        <div className="flex gap-6">
          <SideMenu />

          <main className="min-w-0 flex-1">
            <Outlet />
          </main>
        </div>
      </div>

      {/* 탭 밖에 둔다. 어느 화면에서든 같은 자리에서 열리고 상태가 이어진다 */}
      <QuickAddLauncher />
    </QuickAddProvider>
  );
}
