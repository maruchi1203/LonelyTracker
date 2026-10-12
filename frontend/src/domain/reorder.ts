/**
 * 위·아래 한 칸 움직이기를 서버가 받는 모양으로 바꾼다.
 *
 * 서버의 PATCH /order 는 "이 무리의 최종 구성원 전부"를 받는다. 화면이 아는 것은
 * "이것을 한 칸 올려 달라"뿐이라, 그 사이를 메우는 것이 이 파일의 일이다.
 *
 * 순수 함수로 떼어 둔 까닭은 리스트 항목·습관·카테고리 셋이 같은 계산을 쓰기 때문이다.
 * 화면마다 다시 쓰면 세 군데가 조금씩 달라진다.
 */

/** 움직일 방향. 숫자로 두면 두 칸 이동도 같은 식으로 풀린다 */
export type Step = -1 | 1;

/**
 * id 하나를 step 칸 움직인 새 목록.
 *
 * 원본을 고치지 않고 새 배열을 돌려준다. 호출하는 쪽이 이 결과를 그대로 서버에 보내고,
 * 성공하면 목록을 다시 불러온다.
 *
 * @param ids  지금 화면에 선 차례
 * @param id   움직일 것
 * @param step -1 이면 앞으로, 1 이면 뒤로
 */
export function moveBy(ids: number[], id: number, step: Step): number[] {
  const idx = ids.indexOf(id);

  // 없는 것을 옮기라는 요청이다. 계산하면 -1 이 맨 뒤를 가리켜 멀쩡한 id 가 빠진다
  if (idx < 0) return ids;

  const newIdx = idx + step;

  if (newIdx < 0) return ids;

  if (newIdx >= ids.length) return ids;

  // 먼저 빼야 한다. 넣고 빼면 지울 자리가 방향마다 달라진다
  const newIds = [...ids];
  newIds.splice(idx, 1);
  newIds.splice(newIdx, 0, id);

  return newIds;
}

/**
 * 보이는 것들의 새 차례를 무리 전체에 얹는다.
 *
 * 서버는 무리의 전부를 받는데 화면은 일부를 숨길 수 있다(그만둔 습관). 보이는 것만
 * 보내면 거절당하고, 전부를 화면 기준으로 옮기면 숨은 것과 자리를 바꿔 눈에 아무
 * 변화가 없다. 그래서 보이는 자리끼리만 채우고 숨은 것은 제 인덱스에 둔다.
 *
 * 숨은 것이 자리를 지키므로, 다시 켜도 그 항목이 있던 데가 그대로다.
 *
 * @param all     그 무리의 전부. 지금 선 차례
 * @param visible 보이는 것들의 새 차례. all 의 부분집합이어야 한다
 */
export function applyVisible(all: number[], visible: number[]): number[] {
  const shown = new Set(visible);
  let next = 0;

  return all.map((id) => (shown.has(id) ? visible[next++] : id));
}

/**
 * 움직일 수 있는지. 단추를 잠그는 데 쓴다.
 *
 * 같은 판단을 moveBy 와 따로 두지 않으려고 moveBy 의 답으로 되묻는다.
 * 움직일 수 없으면 moveBy 가 차례를 바꾸지 않는다는 것에 기댄다.
 */
export function canMove(ids: number[], id: number, step: Step): boolean {
  const at = ids.indexOf(id);

  return at >= 0 && at + step >= 0 && at + step < ids.length;
}
