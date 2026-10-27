-- A database at schema v2 (spec 004): settings, built-ins, a salinha and a linked student.
INSERT INTO settings (key, value, updated_at) VALUES
  ('language', '"pt-BR"', '2026-10-27T12:00:00.000Z');
INSERT INTO place (id, builtin, name, kind, payer, rate_type, rate_cents, pay_cycle, created_at, updated_at) VALUES
  ('00000000-0000-7000-8000-000000000002', 'online', NULL, 'online', 'student', 'hour', 10000, 'lesson', '2026-10-27T12:00:00.000Z', '2026-10-27T12:00:00.000Z'),
  ('p1', NULL, 'Salinha Aprender+', 'salinha', 'place', 'hour', 6000, 'monthly', '2026-10-27T12:00:00.000Z', '2026-10-27T12:00:00.000Z');
INSERT INTO student (id, name, color, rate_type, created_at, updated_at) VALUES
  ('s1', 'Ana Souza', 'amber', 'hour', '2026-10-27T12:00:00.000Z', '2026-10-27T12:00:00.000Z');
INSERT INTO student_place (student_id, place_id, is_default, rate_cents, created_at, updated_at) VALUES
  ('s1', 'p1', 1, NULL, '2026-10-27T12:00:00.000Z', '2026-10-27T12:00:00.000Z');
