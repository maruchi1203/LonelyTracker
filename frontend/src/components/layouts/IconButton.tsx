import type { ComponentPropsWithoutRef, ReactNode } from "react";

interface Props extends ComponentPropsWithoutRef<"button"> {
  /** 그림뿐이라 이름을 말로 따로 준다. 손가락에는 툴팁, 화면낭독기에는 이름이 된다 */
  label: string;
  /** 켜고 끄는 단추일 때만. 안 주면 누를 때마다 도는 단추가 아니다 */
  pressed?: boolean;
  /** solid 는 그 줄에서 가장 중요한 하나에만 쓴다 */
  tone?: "solid" | "plain";
  /** 가로로 늘여 그림 옆에 이름까지 보인다. 좁은 자리에서는 그림만 남긴다 */
  wide?: boolean;
  children: ReactNode;
}

const SHELL =
  "flex items-center transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-line disabled:cursor-not-allowed disabled:opacity-40";

/** 늘인 것은 한 줄을 나눠 가지므로 flex 줄 안에 두어야 한다. 동그란 것은 줄어들지 않는다 */
const SHAPE = {
  wide: "min-w-0 flex-1 justify-start gap-2 rounded-full px-4 py-2.5 text-sm font-semibold",
  round: "size-10 shrink-0 justify-center rounded-full",
};

export default function IconButton({
  label,
  pressed,
  tone = "plain",
  wide,
  children,
  className = "",
  ...rest
}: Props) {
  const look =
    tone === "solid"
      ? "bg-accent text-canvas hover:bg-ink"
      : pressed
        ? "border border-accent bg-accent-soft text-accent"
        : "border border-line text-ink-soft hover:bg-surface-soft hover:text-ink";

  return (
    <button
      type="button"
      // 글자가 이미 보이면 툴팁은 같은 말을 두 번 하는 것이다
      title={wide ? undefined : label}
      aria-label={label}
      aria-pressed={pressed}
      className={`${SHELL} ${wide ? SHAPE.wide : SHAPE.round} ${look} ${className}`}
      {...rest}
    >
      {children}
      {wide && <span className="truncate">{label}</span>}
    </button>
  );
}
