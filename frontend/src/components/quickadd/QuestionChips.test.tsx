// @vitest-environment jsdom
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PARSE_QUESTION_TEXT } from '../../constants/parseQuestions'
import QuestionChips from './QuestionChips'

describe('되물음 칩', () => {
  it('질문을 누르면 답할 입력칸으로 데려간다', () => {
    const onFocusField = vi.fn()

    render(
      <QuestionChips
        questions={['START_TIME', 'PLACE']}
        onFocusField={onFocusField}
        onDismiss={vi.fn()}
      />,
    )

    fireEvent.click(screen.getByText(PARSE_QUESTION_TEXT.PLACE))

    expect(onFocusField).toHaveBeenCalledWith('place')
  })

  it('날짜 질문은 시작 시각 칸을 가리킨다', () => {
    const onFocusField = vi.fn()

    render(
      <QuestionChips questions={['DATE']} onFocusField={onFocusField} onDismiss={vi.fn()} />,
    )
    fireEvent.click(screen.getByText(PARSE_QUESTION_TEXT.DATE))

    expect(onFocusField).toHaveBeenCalledWith('startAt')
  })

  it('닫으면 그 질문만 알린다', () => {
    const onDismiss = vi.fn()

    render(
      <QuestionChips
        questions={['CATEGORY']}
        onFocusField={vi.fn()}
        onDismiss={onDismiss}
      />,
    )
    fireEvent.click(screen.getByLabelText('이 질문 닫기'))

    expect(onDismiss).toHaveBeenCalledWith('CATEGORY')
  })

  it('질문이 없으면 아무것도 그리지 않는다', () => {
    const { container } = render(
      <QuestionChips questions={[]} onFocusField={vi.fn()} onDismiss={vi.fn()} />,
    )

    expect(container.firstChild).toBeNull()
  })
})
