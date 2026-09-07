-- 분류를 태그로 바꾼다. 태그는 일정 단위라 회차별 덮어쓰기를 두지 않는다.
-- 마스터 테이블 없이 문자열만 담는다.
CREATE TABLE schedule_tag (
    schedule_id BIGINT      NOT NULL REFERENCES schedule(id) ON DELETE CASCADE,
    name        VARCHAR(50) NOT NULL,
    PRIMARY KEY (schedule_id, name)
);

CREATE INDEX idx_schedule_tag_name ON schedule_tag (name);

DROP INDEX IF EXISTS idx_schedule_category;
ALTER TABLE schedule DROP COLUMN category;
ALTER TABLE schedule_progress DROP COLUMN category;
