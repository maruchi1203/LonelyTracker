-- ScheduleSource 삭제와 description(마크다운) / category(계층 경로) 확장 반영.
--
-- ddl-auto: update 는 컬럼 삭제와 타입 변경을 하지 않아 손으로 처리해야 했던 부분이다.
-- 기존 DB에는 Hibernate가 이미 만들어둔 것이 있을 수 있어 IF EXISTS / IF NOT EXISTS 로 방어한다.

-- 1) 일정의 성격 구분은 category 로 대체한다
ALTER TABLE schedule DROP COLUMN IF EXISTS source;

-- 2) description 은 마크다운 원문을 담으므로 길이 제한을 없앤다
ALTER TABLE schedule ALTER COLUMN description TYPE TEXT;

-- 3) category 는 역슬래시로 계층을 표현한다 (예: 능력\개발\SpringBoot)
ALTER TABLE schedule ALTER COLUMN category TYPE VARCHAR(100);

-- 4) 카테고리 필터는 정확일치 + 하위경로 prefix LIKE 로 조회한다.
--    prefix LIKE 는 앞이 고정이라 이 인덱스를 탈 수 있다.
CREATE INDEX IF NOT EXISTS idx_schedule_category ON schedule (category);
