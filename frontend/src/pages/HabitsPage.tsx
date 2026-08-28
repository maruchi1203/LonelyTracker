import ComingSoon from "../components/ComingSoon";

export default function HabitsPage() {
  return (
    <ComingSoon
      title="습관일지"
      summary="그날 있었던 일을 적으면 들일 습관과 버릴 습관을 AI가 정리해 줍니다."
      blockedBy="하루 한 줄을 담는 daily_log 테이블이 먼저 필요합니다."
    />
  );
}
