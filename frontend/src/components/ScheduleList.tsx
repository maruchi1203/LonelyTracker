import type { Schedule, ScheduleStatus } from '../types/schedule'

interface Props {
  schedules: Schedule[]
  onToggleStatus: (schedule: Schedule) => void
  onDelete: (id: number) => void
}

const STATUS_LABEL: Record<ScheduleStatus, string> = {
  PLANNED: '예정',
  DONE: '완료',
  SKIPPED: '건너뜀',
}

function formatRange(schedule: Schedule): string {
  const start = new Date(schedule.startAt)
  const time = (d: Date) =>
    `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  const date = `${start.getMonth() + 1}/${start.getDate()}`

  if (!schedule.endAt) return `${date} ${time(start)}`
  return `${date} ${time(start)} ~ ${time(new Date(schedule.endAt))}`
}

export default function ScheduleList({ schedules, onToggleStatus, onDelete }: Props) {
  if (schedules.length === 0) {
    return <p className="empty">등록된 일정이 없습니다. 위에서 첫 일정을 추가해 보세요.</p>
  }

  return (
    <ul className="schedule-list">
      {schedules.map((schedule) => (
        <li key={schedule.id} className={schedule.status === 'DONE' ? 'done' : undefined}>
          <input
            type="checkbox"
            checked={schedule.status === 'DONE'}
            onChange={() => onToggleStatus(schedule)}
            aria-label={`${schedule.title} 완료 표시`}
          />

          <div className="body">
            <span className="title">{schedule.title}</span>
            <span className="meta">
              {formatRange(schedule)}
              {schedule.category && (
                <span className="tag" title={schedule.category.path}>
                  {schedule.category.name}
                </span>
              )}
              <span className="status">{STATUS_LABEL[schedule.status]}</span>
            </span>
          </div>

          <button type="button" className="delete" onClick={() => onDelete(schedule.id)}>
            삭제
          </button>
        </li>
      ))}
    </ul>
  )
}
