-- 연기를 별도 동작으로 두지 않는다.
-- 회차가 옮겨졌는지는 on_date(규칙이 만든 원래 날짜)와 start_at 의 차이로 알 수 있어
-- 횟수를 따로 세지 않는다. 평가는 status(SKIPPED)가 맡는다.
ALTER TABLE schedule_progress DROP COLUMN postpone_count;
