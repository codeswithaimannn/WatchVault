-- Cached TMDB title details (API-sourced data, kept apart from user data).
-- TMDB terms: nothing older than 6 months, and everything deleted if API access ends.
-- User tables (library etc.) store only media_type + tmdb_id and never reference this table,
-- because rows here are deleted on a schedule.
CREATE TABLE title_cache (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    media_type VARCHAR(10) NOT NULL,
    tmdb_id    INT         NOT NULL,
    payload    JSON        NOT NULL,
    fetched_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_title_cache PRIMARY KEY (id),
    CONSTRAINT uk_title_cache_title UNIQUE (media_type, tmdb_id),
    CONSTRAINT ck_title_cache_media_type CHECK (media_type IN ('MOVIE', 'TV'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- The daily purge deletes by age.
CREATE INDEX ix_title_cache_fetched_at ON title_cache (fetched_at);
