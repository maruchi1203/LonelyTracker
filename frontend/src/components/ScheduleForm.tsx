import { useState } from 'react'
import type { ScheduleCreateRequest } from '../types/schedule'

interface Props {
  onSubmit: (body: ScheduleCreateRequest) => Promise<void>
  /** 자동완성 후보. 이미 쓰인 카테고리를 그대로 제안한다 */
  knownCategories: string[]
  disabled?: boolean
}

export default function ScheduleForm({ onSubmit, knownCategories, disabled }: Props) {
  const [title, setTitle] = useState('')
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')
  const [category, setCategory] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault() // 폼 기본 동작(페이지 새로고침)을 막는다
    if (!title.trim() || !startAt) return

    await onSubmit({
      title: title.trim(),
      // datetime-local은 "2026-08-19T15:00" 형식이라 초를 붙여 백엔드 LocalDateTime에 맞춘다
      startAt: `${startAt}:00`,
      endAt: endAt ? `${endAt}:00` : undefined,
      category: category || undefined,
    })

    setTitle('')
    setStartAt('')
    setEndAt('')
    setCategory('')
  }

  return (
    <form className="schedule-form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="title">제목</label>
        <input
          id="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="무엇을 할 계획인가요?"
          maxLength={200}
          required
        />
      </div>

      <div className="row">
        <div className="field">
          <label htmlFor="startAt">시작</label>
          <input
            id="startAt"
            type="datetime-local"
            value={startAt}
            onChange={(e) => setStartAt(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="endAt">종료 (선택)</label>
          <input
            id="endAt"
            type="datetime-local"
            value={endAt}
            onChange={(e) => setEndAt(e.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="category">분류 (선택)</label>
          {/* 자유 입력. 역슬래시로 하위 분류를 만든다 — 예: 능력\개발 */}
          <input
            id="category"
            list="category-options"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="예: 능력\개발"
            maxLength={100}
          />
          <datalist id="category-options">
            {knownCategories.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </div>
      </div>

      <button type="submit" disabled={disabled}>
        일정 추가
      </button>
    </form>
  )
}
