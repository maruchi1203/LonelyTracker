import { describe, expect, it } from 'vitest'
import { knownQuestions } from './parseQuestions'

describe('knownQuestions', () => {
  it('모르는 ID 는 버린다', () => {
    expect(knownQuestions(['DATE', 'NEW_ONE'])).toEqual(['DATE'])
  })

  it('폼에 칸이 없는 질문은 올리지 않는다', () => {
    // 장소와 반복 종료일은 폼에서 빠졌다
    expect(knownQuestions(['PLACE', 'RECUR_END', 'START_TIME'])).toEqual([
      'START_TIME',
    ])
  })

  it('없으면 빈 목록이다', () => {
    expect(knownQuestions(undefined)).toEqual([])
  })
})
