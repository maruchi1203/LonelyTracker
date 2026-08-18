import { CATEGORY_SEPARATOR } from '../types/schedule'

interface Props {
  categories: string[]
  selected: string | null
  onSelect: (category: string | null) => void
}

/**
 * 카테고리 필터. 선택하면 하위 카테고리까지 함께 조회된다(필터링은 서버가 수행).
 * 계층은 들여쓰기 깊이로 표현한다.
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

      {categories.map((category) => {
        const depth = category.split(CATEGORY_SEPARATOR).length - 1
        const leaf = category.split(CATEGORY_SEPARATOR).at(-1)
        return (
          <button
            key={category}
            type="button"
            className={selected === category ? 'active' : undefined}
            style={{ marginLeft: `${depth * 0.75}rem` }}
            onClick={() => onSelect(category)}
            title={category}
          >
            {leaf}
          </button>
        )
      })}
    </nav>
  )
}
