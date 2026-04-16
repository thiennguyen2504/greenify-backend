CREATE SCHEMA IF NOT EXISTS public;

ALTER TABLE IF EXISTS green_action_posts
	ADD COLUMN IF NOT EXISTS location TEXT;

-- Review schema updates: simplify reject fields and allow re-review cycles.
ALTER TABLE IF EXISTS post_reviews
	ADD COLUMN IF NOT EXISTS reject_reason TEXT;

DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = 'public'
		  AND table_name = 'post_reviews'
		  AND column_name = 'reject_reason_note'
	) THEN
		UPDATE post_reviews
		SET reject_reason = COALESCE(reject_reason, reject_reason_note)
		WHERE reject_reason IS NULL;
	END IF;

	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = 'public'
		  AND table_name = 'post_reviews'
		  AND column_name = 'reject_reason_code'
	) THEN
		UPDATE post_reviews
		SET reject_reason = COALESCE(reject_reason, reject_reason_code)
		WHERE reject_reason IS NULL;
	END IF;
END $$;

ALTER TABLE IF EXISTS post_reviews
	DROP CONSTRAINT IF EXISTS uk_post_reviewer;

ALTER TABLE IF EXISTS post_reviews
	DROP COLUMN IF EXISTS reject_reason_code,
	DROP COLUMN IF EXISTS reject_reason_note;

-- Appeal schema (business case: post appeal after rejected decision).
CREATE TABLE IF NOT EXISTS post_appeals (
	id VARCHAR(255) PRIMARY KEY,
	ol BIGINT NOT NULL DEFAULT 0,
	created_at TIMESTAMPTZ,
	created_by VARCHAR(255),
	last_modified_at TIMESTAMPTZ,
	last_modified_by VARCHAR(255),
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	post_id VARCHAR(255) NOT NULL,
	user_id VARCHAR(255) NOT NULL,
	appeal_reason TEXT NOT NULL,
	evidence_urls JSONB,
	attempt_number INT NOT NULL,
	status VARCHAR(30) NOT NULL,
	admin_note TEXT,
	CONSTRAINT fk_post_appeals_post FOREIGN KEY (post_id) REFERENCES green_action_posts(id),
	CONSTRAINT fk_post_appeals_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_post_appeals_post_id ON post_appeals (post_id);
CREATE INDEX IF NOT EXISTS idx_post_appeals_user_id ON post_appeals (user_id);
CREATE INDEX IF NOT EXISTS idx_post_appeals_status ON post_appeals (status);
