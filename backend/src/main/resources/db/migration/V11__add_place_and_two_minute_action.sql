-- 습관 기록의 네 칸 중 장소와 2분 행동을 담을 자리. 둘 다 선택 입력이라 NULL 을 허용한다.
ALTER TABLE schedule ADD COLUMN place VARCHAR(200);
ALTER TABLE schedule ADD COLUMN two_minute_action VARCHAR(200);
