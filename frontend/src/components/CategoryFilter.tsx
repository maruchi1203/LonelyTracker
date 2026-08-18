import type { Category } from '../types/schedule'

interface Props {
  categories: Category[]
  selected: string | null
  onSelect: (path: string | null) => void
}

/**
 * 카테고리 사이드바. 선택하면 하위 카테고리까지 함께 조회된다(필터링은 서버가 수행).
 * 목록이 경로순으로 오므로 그대로 그리면 부모가 자식보다 먼저 나온다.
 */
export default function CategoryFilter({ categories, selected, onSelect }: Props) {
  if (categories.length === 0) return null

  return (
    <nav className="category-filter" aria-label="카테고리 필터">
      <button
        type="button"
        className={selected === null ? 'active' : undefined}
        onClick={() => onSelect(null)}
      >
        전체
      </button>

      {categories
        .filter((c) => !c.archived)
        .map((category) => (
          <button
            key={category.id}
            type="button"
            className={selected === category.path ? 'active' : undefined}
            style={{
              marginLeft: `${category.depth * 0.75}rem`,
              borderColor: category.color ?? undefined,
            }}
            onClick={() => onSelect(category.path)}
            title={category.path}
          >
            {category.name}
          </button>
        ))}
    </nav>
  )
}
