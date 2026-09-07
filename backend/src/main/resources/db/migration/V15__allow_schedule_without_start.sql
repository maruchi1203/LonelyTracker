-- 날짜를 아직 안 정한 항목을 담기 위해 시작일시를 선택으로 바꾼다.
-- start_at 이 없으면 펼칠 기준이 없어 회차가 0개이고, 달력에는 나오지 않는다.
ALTER TABLE schedule ALTER COLUMN start_at DROP NOT NULL;
