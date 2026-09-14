-- AI 제공자마다 키를 따로 둔다. 제공자를 바꿔도 앞서 넣은 키가 남는다
CREATE TABLE ai_credential (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    -- 제공자를 가르는 값. 목록을 두지 않아 어느 호환 제공자든 들어온다
    base_url   VARCHAR(300) NOT NULL,
    model      VARCHAR(100) NOT NULL,
    -- AES-GCM 암호문. 원본보다 길어 넉넉히 잡는다
    api_key    VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_ai_credential_base_url UNIQUE (user_id, base_url)
);

-- 지금 쓰는 자격 증명. 비어 있으면 서버 설정(.env)을 쓴다
ALTER TABLE app_user ADD COLUMN active_ai_credential_id BIGINT
    REFERENCES ai_credential (id) ON DELETE SET NULL;

-- 이미 넣어 둔 OpenAI 키를 첫 자격 증명으로 옮긴다
-- 같은 마스터 키로 만든 암호문이라 그대로 옮겨도 읽힌다
INSERT INTO ai_credential (user_id, base_url, model, api_key, created_at, updated_at)
SELECT id, 'https://api.openai.com/v1', 'gpt-5.6-luna', openai_api_key, now(), now()
FROM app_user
WHERE openai_api_key IS NOT NULL;

UPDATE app_user u
SET active_ai_credential_id = c.id
FROM ai_credential c
WHERE c.user_id = u.id;

ALTER TABLE app_user DROP COLUMN openai_api_key;
