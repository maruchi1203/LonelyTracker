-- 일정마다 MoSCoW 우선순위를 붙인다.
-- 기본값을 두지 않는다. 값이 없으면 Could have 로 해석하되,
-- "아직 아무도 안 정한 것"과 "일부러 Could 로 정한 것"은 구분할 수 있어야 한다.
-- ver.2 에서 AI 가 일정을 검토하며 채워 줄 수 있다.
ALTER TABLE schedule ADD COLUMN priority VARCHAR(10);
