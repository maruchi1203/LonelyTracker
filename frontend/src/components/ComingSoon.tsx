interface Props {
  title: string;
  /** 이 화면이 무엇을 보여줄 것인지 */
  summary: string;
  /** 먼저 만들어져야 하는 것 */
  blockedBy: string;
}

/** 아직 데이터가 없는 화면의 자리표시자. 가짜 데이터를 넣지 않는다 */
export default function ComingSoon({ title, summary, blockedBy }: Props) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold text-slate-800">{title}</h2>

      <div className="flex flex-col gap-2 rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
        <p className="text-sm text-slate-500">{summary}</p>
        <p className="text-xs text-slate-400">{blockedBy}</p>
      </div>
    </section>
  );
}
