import { useCallback, useEffect, useState } from 'react'
import { changeStatus, createSchedule, deleteSchedule, fetchSchedules } from './api/schedules'
import ScheduleForm from './components/ScheduleForm'
import ScheduleList from './components/ScheduleList'
import type { Schedule, ScheduleCreateRequest } from './types/schedule'
import './App.css'

export default function App() {
  const [schedules, setSchedules] = useState<Schedule[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setSchedules(await fetchSchedules())
    } catch (e) {
      setError(e instanceof Error ? e.message : '목록을 불러오지 못했습니다')
    } finally {
      setLoading(false)
    }
  }, [])

  // 화면이 처음 뜰 때 한 번 목록을 불러온다
  useEffect(() => {
    void load()
  }, [load])

  const handleCreate = async (body: ScheduleCreateRequest) => {
    setError(null)
    try {
      const created = await createSchedule(body)
      // 서버가 정렬해서 주므로, 새로 만든 뒤에는 목록을 다시 받아 순서를 맞춘다
      setSchedules((prev) => [...prev, created].sort((a, b) => a.startAt.localeCompare(b.startAt)))
    } catch (e) {
      setError(e instanceof Error ? e.message : '일정을 추가하지 못했습니다')
    }
  }

  const handleToggleStatus = async (schedule: Schedule) => {
    setError(null)
    try {
      const updated = await changeStatus(schedule.id, schedule.status === 'DONE' ? 'PLANNED' : 'DONE')
      setSchedules((prev) => prev.map((s) => (s.id === updated.id ? updated : s)))
    } catch (e) {
      setError(e instanceof Error ? e.message : '상태를 변경하지 못했습니다')
    }
  }

  const handleDelete = async (id: number) => {
    setError(null)
    try {
      await deleteSchedule(id)
      setSchedules((prev) => prev.filter((s) => s.id !== id))
    } catch (e) {
      setError(e instanceof Error ? e.message : '일정을 삭제하지 못했습니다')
    }
  }

  const doneCount = schedules.filter((s) => s.status === 'DONE').length

  return (
    <main className="app">
      <header>
        <h1>일정 관리</h1>
        <p className="summary">
          전체 {schedules.length}건 · 완료 {doneCount}건
        </p>
      </header>

      <ScheduleForm onSubmit={handleCreate} disabled={loading} />

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
