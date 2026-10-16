-- The name shown in the app ("Welcome, Sam"). Not unique: two people can share a name.
-- Added in three steps so existing accounts get a value before the column becomes NOT NULL.
ALTER TABLE users ADD COLUMN display_name VARCHAR(50) NULL AFTER email;

-- Existing accounts: use the part of the email before the @ until the user sets a real name.
UPDATE users SET display_name = LEFT(SUBSTRING_INDEX(email, '@', 1), 50);

ALTER TABLE users MODIFY COLUMN display_name VARCHAR(50) NOT NULL;
