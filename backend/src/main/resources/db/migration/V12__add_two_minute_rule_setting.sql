-- 폼에 2분 행동 칸을 띄울지에 대한 사용자 기본값. 습관 도구라 켬으로 시작한다.
ALTER TABLE app_user ADD COLUMN two_minute_rule BOOLEAN NOT NULL DEFAULT TRUE;
