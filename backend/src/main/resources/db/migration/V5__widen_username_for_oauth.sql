-- 로그인을 OAuth로만 받기로 하면서, username 에 제공자가 주는 이메일이나 식별자가 들어간다.
-- 이메일은 50자를 넘길 수 있어 컬럼을 넓힌다.
--
-- 애플리케이션 쪽 길이 제한은 FieldLengths.USERNAME 에 있다. 두 값은 항상 같아야 한다.

ALTER TABLE app_user ALTER COLUMN username TYPE VARCHAR(100);
