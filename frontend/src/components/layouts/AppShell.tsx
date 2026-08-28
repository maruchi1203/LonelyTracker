import { Outlet } from "react-router";
import Header from "./Header";
import SideMenu from "./SideMenu";

/** 화면 뼈대. 헤더 + 좌측 메뉴 + 본문 배치만 담당한다 */
export default function AppShell() {
  return (
    <div className="mx-auto max-w-6xl px-5 pt-8 pb-20">
      <Header />

      <div className="flex gap-6">
        <SideMenu />

        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
