-- 이번 달 토큰이 이 값을 넘으면 그 제공자로는 부르지 않는다
-- NULL 이면 한도 없음. 이미 있는 줄은 전부 NULL 로 들어간다
ALTER TABLE ai_provider ADD COLUMN monthly_token_limit INT;
