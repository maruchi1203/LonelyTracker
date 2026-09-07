-- 1회성 일정의 완료를 일정 자체에 적는다.
-- 회차별 status 는 습관 전용으로 남아, 한 일정의 완료 출처가 항상 하나가 된다.
ALTER TABLE schedule ADD COLUMN completed_at TIMESTAMP(6);

-- 반복 규칙이 없는 일정의 DONE 회차를 옮긴다.
UPDATE schedule s
   SET completed_at = p.updated_at
  FROM schedule_progress p
 WHERE p.schedule_id = s.id
   AND p.status = 'DONE'
   AND NOT EXISTS (SELECT 1 FROM schedule_recur r WHERE r.schedule_id = s.id);
