import type { CategoryResponse } from "../../types/schedule";
import Header from "./Header";
import SideMenu from "./SideMenu";

interface Props {
  categories: CategoryResponse[];
  selectedCategory: string | null;
  onSelectCategory: (name: string | null) => void;
  /** 본문에 무엇을 넣을지는 화면을 쓰는 쪽이 정한다 */
  children: React.ReactNode;
}

/**
 * 화면 뼈대. 헤더 + 좌측 분류 메뉴 + 본문 배치만 담당한다.
 * <p>
 * 본문 내용을 직접 알지 않고 children 으로 받는다. 달력을 넣든 목록을 넣든
 * 이 파일은 바뀌지 않는다. 달력이 데이터를 필요로 하는데, 그 데이터를 들고 있는 쪽은
 * App 이므로 달력도 App 이 그려서 children 으로 넘긴다.
 */
export default function AppShell({
  categories,
  selectedCategory,
  onSelectCategory,
  children,
}: Props) {
  return (
    <div className="mx-auto max-w-6xl px-5 pt-8 pb-20">
      <Header />

      <div className="flex gap-6">
        <SideMenu
          categories={categories}
          selected={selectedCategory}
          onSelect={onSelectCategory}
        />

        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  );
}
