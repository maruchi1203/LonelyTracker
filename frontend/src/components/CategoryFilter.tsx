import type { Category } from '../types/schedule'

interface Props {
  categories: Category[]
  selected: string | null
  onSelect: (name: string | null) => void
}

/**
 * 카테고리 필터. 계층이 없어져 이름이 정확히 일치하는 일정만 조회한다.
 * 목록은 표시 순서대로 오므로 그대로 그린다.
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
            className={selected === category.name ? 'active' : undefined}
            style={{ borderColor: category.color ?? undefined }}
            onClick={() => onSelect(category.name)}
          >
            {category.name}
          </button>
        ))}
    </nav>
  )
}
