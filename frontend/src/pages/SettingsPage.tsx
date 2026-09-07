import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import { fetchMe } from "../api/users";
import CategorySection from "../components/settings/CategorySection";
import OpenAiKeySection from "../components/settings/OpenAiKeySection";
import TwoMinuteRuleSection from "../components/settings/TwoMinuteRuleSection";
import type { UserResponse } from "../types/schedule";

export default function SettingsPage() {
  const [me, setMe] = useState<UserResponse | null>(null);
  const [params] = useSearchParams();

  useEffect(() => {
    void fetchMe()
      .then(setMe)
      .catch(() => {
        // 계정 표시는 부가 정보라 실패해도 다른 설정을 막지 않는다
      });
  }, []);

  return (
    <div className="flex flex-col gap-5">
      <h2 className="text-lg font-semibold text-slate-800">설정</h2>

      <section className="flex flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
        <h3 className="font-semibold text-slate-800">계정</h3>
        <p className="text-sm text-slate-700">
          {me ? (me.displayName ?? me.username) : "…"}
        </p>
        <p className="text-xs text-slate-400">
          로그인이 아직 없어 단일 사용자로 동작합니다.
        </p>
      </section>

      <OpenAiKeySection autoFocus={params.get("focus") === "openai-key"} />

      <CategorySection />

      <TwoMinuteRuleSection />
    </div>
  );
}
