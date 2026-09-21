import { Link } from "react-router";

export default function NotFoundPage() {
  return (
    <section className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-line bg-surface px-6 py-16 text-center">
      <p className="text-sm text-ink-soft">없는 페이지입니다</p>
      <Link
        to="/calendar"
        className="rounded-md bg-accent px-4 py-2 text-sm font-semibold text-canvas hover:bg-ink"
      >
        달력으로 가기
      </Link>
    </section>
  );
}
