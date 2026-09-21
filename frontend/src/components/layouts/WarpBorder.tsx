import { useId, type ComponentPropsWithoutRef, type ReactNode } from "react";

/** 물결의 결. 작을수록 크고 느린 파도, 클수록 잔물결이다 */
const FREQUENCY = ["0.009 0.013", "0.014 0.008", "0.009 0.013"].join(";");

/** 한 바퀴 도는 데 걸리는 시간. 길수록 잔잔하다 */
const PERIOD = "16s";

/** 선이 밀려나는 최대 거리(px) */
const DEPTH = 8;

interface Props extends ComponentPropsWithoutRef<"div"> {
  children: ReactNode;
  /** 바깥에서 주는 여백·바탕색 같은 것 */
  className?: string;
}

/**
 * 테두리가 물결처럼 일렁이는 상자.
 * 선 색은 부모의 글자색을, 모서리는 부모의 radius 를 따른다
 */
export default function WarpBorder({
  children,
  className = "",
  ...rest
}: Props) {
  // 칸마다 다른 id 를 줘서 물결이 서로 어긋나게 인다
  const id = `ripple-${useId().replace(/:/g, "")}`;
  const calm = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  return (
    <div className={`relative ${className}`} {...rest}>
      <svg className="absolute size-0" aria-hidden="true">
        <filter id={id}>
          {/* 노이즈를 만들고 */}
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.01 0.02"
            numOctaves="1"
            result="noise"
          >
            {!calm && (
              <animate
                attributeName="baseFrequency"
                values={FREQUENCY}
                dur={PERIOD}
                repeatCount="indefinite"
              />
            )}
          </feTurbulence>
          {/* 그 노이즈만큼 선을 밀어낸다 */}
          <feDisplacementMap in="SourceGraphic" in2="noise" scale={DEPTH} />
        </filter>
      </svg>

      {/* 테두리만 가진 빈 겹. 필터가 이 겹에만 걸려 글자는 흔들리지 않는다 */}
      <div
        className="pointer-events-none absolute inset-0 rounded-[inherit] border-2 border-current"
        style={{ filter: `url(#${id})` }}
        aria-hidden="true"
      />

      {children}
    </div>
  );
}
