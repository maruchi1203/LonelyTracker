-- OpenAI API 키를 사용자별로 갖는다.
--
-- 서버 설정(.env)에 두면 서버가 모든 사용자의 사용료를 대신 내게 되고,
-- 사용량도 쓰는 사람에게 귀속되지 않는다.
-- DB 접속 정보는 반대다 - 서버가 하나의 DB 를 쓰므로 서버 설정에 남는다.
--
-- 값은 AES-GCM 으로 암호화해 저장한다. base64(iv || ciphertext) 라
-- 원본(약 50자)보다 길어지므로 넉넉히 잡는다.
ALTER TABLE app_user ADD COLUMN openai_api_key VARCHAR(500);
