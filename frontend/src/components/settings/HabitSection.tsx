import { useEffect, useState } from "react";
import { changeSettings, fetchSettings } from "../../api/users";

/** 2분 행동 칸을 폼에 늘 띄울지 정한다 */
export default function HabitSection() {
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void fetchSettings()
      .then((s) => setEnabled(s.twoMinuteRule))
      .catch(() => setError("설정을 불러오지 못했습니다"));
  }, []);

  const toggle = async (next: boolean) => {
    setBusy(true);
    setError(null);
    try {
      const saved = await changeSettings({ twoMinuteRule: next });
      setEnabled(saved.twoMinuteRule);
    } catch (e) {
      setError(e instanceof Error ? e.message : "설정을 바꾸지 못했습니다");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="flex flex-col gap-2 rounded-2xl border border-line bg-surface p-5 shadow-xs">
      <h3 className="font-semibold text-ink">습관일지</h3>

      <label className="flex cursor-pointer items-center gap-2 text-sm text-ink select-none">
        <input
          type="checkbox"
          className="size-4 cursor-pointer accent-accent"
          checked={enabled ?? true}
          disabled={enabled === null || busy}
          onChange={(e) => void toggle(e.target.checked)}
        />
        2분 법칙 적용 여부
      </label>
      <p className="text-xs text-ink-faint">
        시작에 필요한 2분 이내의 행동을 미리 정해두면 실행될 확률이 높아집니다.
        꺼도 이미 적어둔 2분 행동은 그대로 보입니다.
      </p>

      {error && <p className="text-sm text-danger">{error}</p>}
    </section>
  );
}
