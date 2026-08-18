import { useCallback, useEffect, useState } from 'react'
import { changeStatus, createSchedule, deleteSchedule, fetchSchedules } from './api/schedules'
import CategoryFilter from './components/CategoryFilter'
import ScheduleForm from './components/ScheduleForm'
import ScheduleList from './components/ScheduleList'
import { collectCategories, type Schedule, type ScheduleCreateRequest } from './types/schedule'
import './App.css'

export default function App() {
  const [schedules, setSchedules] = useState<Schedule[]>([])
  const [categories, setCategories] = useState<string[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback)

  /** 카테고리 목록은 필터와 무관하게 전체 기준으로 뽑아야 선택 후에도 남아 있다 */
  const loadCategories = useCallback(async () => {
    try {
      setCategories(collectCategories(await fetchSchedules()))
    } catch {
      // 목록 조회 쪽에서 이미 에러를 보여주므로 여기서는 조용히 넘어간다
    }
  }, [])

  const load = useCallback(async (category: string | null) => {
    setLoading(true)
    setError(null)
    try {
      setSchedules(await fetchSchedules(category ? { category } : undefined))
    } catch (e) {
      fail(e, '목록을 불러오지 못했습니다')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load(selectedCategory)
  }, [load, selectedCategory])

  useEffect(() => {
    void loadCategories()
  }, [loadCategories])

  const handleCreate = async (body: ScheduleCreateRequest) => {
    setError(null)
    try {
      await createSchedule(body)
      // 새 일정이 현재 필터에 맞는지는 서버가 판단하므로 다시 받아온다
      await Promise.all([load(selectedCategory), loadCategories()])
    } catch (e) {
      fail(e, '일정을 추가하지 못했습니다')
    }
  }

  const handleToggleStatus = async (schedule: Schedule) => {
    setError(null)
    try {
      const updated = await changeStatus(schedule.id, schedule.status === 'DONE' ? 'PLANNED' : 'DONE')
      setSchedules((prev) => prev.map((s) => (s.id === updated.id ? updated : s)))
    } catch (e) {
      fail(e, '상태를 변경하지 못했습니다')
    }
  }

  const handleDelete = async (id: number) => {
    setError(null)
    try {
      await deleteSchedule(id)
      setSchedules((prev) => prev.filter((s) => s.id !== id))
      await loadCategories()
    } catch (e) {
      fail(e, '일정을 삭제하지 못했습니다')
    }
  }

  const doneCount = schedules.filter((s) => s.status === 'DONE').length

  return (
    <main className="app">
      <header>
        <h1>일정 관리</h1>
        <p className="summary">
          {selectedCategory && <span className="scope">{selectedCategory} · </span>}
          전체 {schedules.length}건 · 완료 {doneCount}건
        </p>
      </header>

      <ScheduleForm onSubmit={handleCreate} knownCategories={categories} disabled={loading} />

      <CategoryFilter
        categories={categories}
        selected={selectedCategory}
        onSelect={setSelectedCategory}
      />

      {error && <p className="error">{error}</p>}

      {loading ? (
        <p className="empty">불러오는 중…</p>
      ) : (
        <ScheduleList
          schedules={schedules}
          onToggleStatus={handleToggleStatus}
          onDelete={handleDelete}
        />
      )}
    </main>
  )
}
