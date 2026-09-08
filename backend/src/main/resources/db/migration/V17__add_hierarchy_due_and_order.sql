-- 리스트 탭이 필요한 세 칸.
-- parent_id 로 상위 일정을 걸고, due_on 으로 기한을 적고, display_order 로 형제 사이 순서를 매긴다.
-- 부모를 지우면 자식은 남아 최상위로 올라간다. 딸린 일정이 함께 사라지면 안 된다.
ALTER TABLE schedule ADD COLUMN parent_id     BIGINT REFERENCES schedule(id) ON DELETE SET NULL;
ALTER TABLE schedule ADD COLUMN due_on        DATE;
ALTER TABLE schedule ADD COLUMN display_order INT NOT NULL DEFAULT 0;

CREATE INDEX idx_schedule_parent_id ON schedule (parent_id);
