interface Props {
  title: string;
  /** 이 화면이 무엇을 보여줄 것인지 */
  summary: string;
}

const headerStyle = "text-lg font-semibold";
const letterBoxStyle =
  "flex flex-col gap-2 rounded-2xl border border-dashed border-line bg-surface px-6 py-12 text-center";
const summaryStyle = "text-sm text-ink-soft";

/** 아직 데이터가 없는 화면의 자리표시자. 가짜 데이터를 넣지 않는다 */
export default function ComingSoon({ title, summary }: Props) {
  return (
    <section className="flex flex-col gap-3">
      <h2 id="title" className={`${headerStyle} text-ink`}>
        {title}
      </h2>

      <div className={letterBoxStyle}>
        <h3 className={`${headerStyle} text-ink-soft`}>개발 예정</h3>
        <p className={summaryStyle}>{summary}</p>
      </div>
    </section>
  );
}
