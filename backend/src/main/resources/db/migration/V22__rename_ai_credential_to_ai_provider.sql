-- 이 테이블은 키뿐 아니라 모델 같은 제공자 설정도 담는다. 담는 것에 맞춰 이름을 바꾼다
-- 행은 그대로 남아 등록해 둔 키를 다시 넣을 필요가 없다
ALTER TABLE ai_credential RENAME TO ai_provider;

ALTER TABLE ai_provider RENAME CONSTRAINT uq_ai_credential_base_url TO uq_ai_provider_base_url;

ALTER TABLE app_user RENAME COLUMN active_ai_credential_id TO active_ai_provider_id;
