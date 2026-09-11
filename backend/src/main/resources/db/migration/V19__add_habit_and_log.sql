-- 습관은 일정과 섞지 않는다. 탭을 붙였다 뗄 수 있어야 한다
CREATE TABLE habit (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title             VARCHAR(200) NOT NULL,
    category          VARCHAR(20)  NOT NULL,
    two_minute_action VARCHAR(200),
    display_order     INT          NOT NULL DEFAULT 0,
    -- 그만둔 습관도 지난 기록이 남아야 해서 지우는 대신 덮어 둔다
    archived_at       TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_habit_user_id ON habit (user_id);

-- 한 날에 한 줄. 줄이 있으면 그날 해낸 것이다
CREATE TABLE habit_log (
    id         BIGSERIAL PRIMARY KEY,
    habit_id   BIGINT    NOT NULL REFERENCES habit (id) ON DELETE CASCADE,
    on_date    DATE      NOT NULL,
    note       VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_habit_log_day UNIQUE (habit_id, on_date)
);

CREATE INDEX idx_habit_log_on_date ON habit_log (on_date);
