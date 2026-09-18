-- AI 호출 한 번에 한 줄. 제공자별로 기간마다 호출 수와 토큰을 센다
-- 석 달이 지난 줄은 새 줄을 적을 때 지운다
CREATE TABLE ai_usage (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    base_url      VARCHAR(300) NOT NULL,
    model         VARCHAR(100) NOT NULL,
    input_tokens  INT          NOT NULL,
    output_tokens INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX idx_ai_usage_user_created ON ai_usage (user_id, created_at);
