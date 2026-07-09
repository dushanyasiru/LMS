-- ============================================================
--  Performance indexes for Stack ICT Academy LMS.
--
--  These match the @Index annotations on the entities. Hibernate's
--  ddl-auto=update creates them automatically on a FRESH database, but it
--  does NOT reliably add indexes to tables that already exist. So on the
--  existing Supabase database, run this ONCE in the Supabase SQL editor
--  (Dashboard -> SQL Editor -> New query -> paste -> Run). It's idempotent
--  (IF NOT EXISTS), so re-running is harmless.
--
--  These speed up the grade-filtered lesson/paper/assignment lists and the
--  per-student / per-class fee & submission lookups as data grows.
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_resource_grade         ON resource (grade);
CREATE INDEX IF NOT EXISTS idx_resource_category      ON resource (category);
CREATE INDEX IF NOT EXISTS idx_resource_lesson        ON resource (lesson_id);

CREATE INDEX IF NOT EXISTS idx_assignment_grade       ON assignment (grade);

CREATE INDEX IF NOT EXISTS idx_submission_student     ON submission (student_id);
CREATE INDEX IF NOT EXISTS idx_submission_assignment  ON submission (assignment_id);

CREATE INDEX IF NOT EXISTS idx_class_session_class    ON class_session (class_id);

CREATE INDEX IF NOT EXISTS idx_class_payment_student  ON class_payment (student_id);

CREATE INDEX IF NOT EXISTS idx_session_payment_student ON session_payment (student_id);
