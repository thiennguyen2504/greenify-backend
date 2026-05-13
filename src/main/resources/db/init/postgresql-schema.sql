CREATE SCHEMA IF NOT EXISTS public;

-- Plantation schema updates: switch from ratios to slot/building.
ALTER TABLE IF EXISTS plantations
	ADD COLUMN IF NOT EXISTS slot_id VARCHAR(50),
	ADD COLUMN IF NOT EXISTS building VARCHAR(30);

ALTER TABLE IF EXISTS plantations
	ALTER COLUMN slot_id SET NOT NULL,
	ALTER COLUMN building SET NOT NULL;

ALTER TABLE IF EXISTS plantations
	DROP COLUMN IF EXISTS x_ratio,
	DROP COLUMN IF EXISTS y_ratio;

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

-- CO2e impact wallet + transactions
CREATE TABLE IF NOT EXISTS green_impact_wallets (
	id VARCHAR(255) PRIMARY KEY,
	ol BIGINT NOT NULL DEFAULT 0,
	created_at TIMESTAMPTZ,
	created_by VARCHAR(255),
	last_modified_at TIMESTAMPTZ,
	last_modified_by VARCHAR(255),
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	user_id VARCHAR(255) NOT NULL,
	total_avoided_kg NUMERIC(12, 4) NOT NULL DEFAULT 0,
	total_absorbed_kg NUMERIC(12, 4) NOT NULL DEFAULT 0,
	CONSTRAINT uk_green_impact_wallet_user UNIQUE (user_id),
	CONSTRAINT fk_green_impact_wallet_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS co2e_transactions (
	id VARCHAR(255) PRIMARY KEY,
	ol BIGINT NOT NULL DEFAULT 0,
	created_at TIMESTAMPTZ,
	created_by VARCHAR(255),
	last_modified_at TIMESTAMPTZ,
	last_modified_by VARCHAR(255),
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	post_id VARCHAR(255) NOT NULL,
	user_id VARCHAR(255) NOT NULL,
	co2e_type VARCHAR(20),
	co2e_kg NUMERIC(12, 4),
	status VARCHAR(20) NOT NULL,
	material_code VARCHAR(50),
	material_label VARCHAR(120),
	confidence_score NUMERIC(6, 4),
	confidence_multiplier NUMERIC(4, 2),
	estimated_weight_kg NUMERIC(12, 4),
	quantity INT,
	skip_reason VARCHAR(120),
	CONSTRAINT uk_co2e_transactions_post UNIQUE (post_id),
	CONSTRAINT fk_co2e_transactions_user FOREIGN KEY (user_id) REFERENCES app_user(id),
	CONSTRAINT fk_co2e_transactions_post FOREIGN KEY (post_id) REFERENCES green_action_posts(id)
);

CREATE INDEX IF NOT EXISTS idx_co2e_transactions_user_id ON co2e_transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_co2e_transactions_status ON co2e_transactions (status);
CREATE INDEX IF NOT EXISTS idx_co2e_transactions_created_at ON co2e_transactions (created_at);
