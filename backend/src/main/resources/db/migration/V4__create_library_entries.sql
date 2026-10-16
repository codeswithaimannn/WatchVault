-- A user's library: user data, kept apart from TMDB data. Only the TMDB id is stored here;
-- titles and posters come from title_cache (TMDB terms: no TMDB data kept beyond 6 months).
CREATE TABLE library_entries (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    media_type   VARCHAR(10)   NOT NULL,
    tmdb_id      INT           NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    rating       TINYINT       NULL,
    notes        VARCHAR(1000) NULL,
    -- Set when the entry becomes COMPLETED, cleared if it moves to another status.
    completed_on DATE          NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_library_entries PRIMARY KEY (id),
    -- One entry per title per user. Starts with user_id, so it also serves "this user's library".
    CONSTRAINT uk_library_entries_title UNIQUE (user_id, media_type, tmdb_id),
    CONSTRAINT fk_library_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_library_entries_media_type CHECK (media_type IN ('MOVIE', 'TV')),
    CONSTRAINT ck_library_entries_status CHECK (status IN ('PLAN_TO_WATCH', 'WATCHING', 'COMPLETED', 'ON_HOLD', 'DROPPED')),
    CONSTRAINT ck_library_entries_rating CHECK (rating BETWEEN 1 AND 10)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
