import { useEffect, useState } from "react";
import { changeSettings, fetchSettings } from "../../api/users";

/** 2분 행동 칸을 폼에 늘 띄울지 정한다 */
export default function TwoMinuteRuleSection() {
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
    <section className="flex flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
      <h3 className="font-semibold text-slate-800">2분 법칙</h3>

      <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700 select-none">
        <input
          type="checkbox"
          className="size-4 cursor-pointer accent-brand-500"
          checked={enabled ?? true}
          disabled={enabled === null || busy}
          onChange={(e) => void toggle(e.target.checked)}
        />
        일정을 적을 때 2분 행동 칸을 띄웁니다
      </label>

      <p className="text-xs text-slate-400">
        시작에 필요한 2분 이내의 행동을 미리 정해두면 실행될 확률이 높아집니다.
        꺼도 이미 적어둔 2분 행동은 그대로 보입니다.
      </p>

      {error && <p className="text-sm text-red-600">{error}</p>}
    </section>
  );
}
