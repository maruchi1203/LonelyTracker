import { Link } from "react-router";

export default function NotFoundPage() {
  return (
    <section className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
      <p className="text-sm text-slate-500">없는 페이지입니다.</p>
      <Link
        to="/calendar"
        className="rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600"
      >
        달력으로 가기
      </Link>
    </section>
  );
}
