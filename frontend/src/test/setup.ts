import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// 렌더한 DOM 을 테스트마다 치운다. 남겨두면 다음 테스트가 이전 화면까지 함께 찾는다
afterEach(cleanup)
