import { canMove, moveBy, type Step } from "../../domain/reorder";

interface Props {
  /** 그 무리가 지금 선 차례. 서버가 받는 목록과 같아야 한다 */
  ids: number[];
  id: number;
  /** 읽어 줄 이름. "운동 위로" 처럼 쓰인다 */
  label: string;
  /** 새 차례. 부르는 쪽이 서버로 보내고 목록을 다시 불러온다 */
  onMove: (ids: number[]) => void;
  /** 보내는 중. 연달아 누르면 먼저 간 요청과 어긋난다 */
  busy?: boolean;
}

const BUTTON =
  "rounded-md border border-line px-1.5 py-1 text-xs leading-none text-ink-soft transition-colors hover:bg-surface-soft disabled:cursor-not-allowed disabled:opacity-30";

/**
 * 위·아래 한 칸 옮기는 단추 한 쌍.
 *
 * 드래그가 아닌 까닭은 터치와 키보드가 함께 되어야 해서다. 단추는 Tab 으로 닿고,
 * 좁은 화면에서도 같은 자리에 있다.
 */
export default function OrderButtons({
  ids,
  id,
  label,
  onMove,
  busy,
}: Props) {
  const button = (step: Step, glyph: string, where: string) => (
    <button
      type="button"
      // 끝에서 잠근다. 서버도 막지만 눌러도 안 되는 단추를 두면 사용자가 헤맨다
      disabled={busy || !canMove(ids, id, step)}
      aria-label={`${label} ${where}`}
      title={where}
      onClick={() => onMove(moveBy(ids, id, step))}
      className={BUTTON}
    >
      {glyph}
    </button>
  );

  return (
    <div className="flex shrink-0 gap-0.5">
      {button(-1, "↑", "위로")}
      {button(1, "↓", "아래로")}
    </div>
  );
}
