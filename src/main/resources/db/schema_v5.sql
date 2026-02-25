ALTER TABLE user_profile ADD COLUMN password_hash TEXT NOT NULL DEFAULT '';
ALTER TABLE user_profile ADD COLUMN active INTEGER NOT NULL DEFAULT 1;

UPDATE user_profile
SET active = 1
WHERE active IS NULL;
