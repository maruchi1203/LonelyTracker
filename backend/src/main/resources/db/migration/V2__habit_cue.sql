-- 2분 법칙의 신호. 언제·어디서를 미리 정해 두면 실행될 확률이 높아진다.
--
-- at_time 을 TIME 이 아니라 글자로 두는 까닭은, 실행 의도의 신호가 시계가 아니라
-- 상황인 경우가 많아서다 — "07:00" 도 "퇴근 후" 도 같은 칸에 들어가야 한다.
ALTER TABLE habit
    ADD COLUMN at_time VARCHAR(100),
    ADD COLUMN place   VARCHAR(200);
