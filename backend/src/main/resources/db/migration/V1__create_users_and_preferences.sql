-- Accounts. One row per registered user.
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- One settings row per user (1:1). Deleted automatically with the user.
CREATE TABLE user_preferences (
    user_id    BIGINT      NOT NULL,
    region     CHAR(2)     NOT NULL DEFAULT 'GB',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                           ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_preferences PRIMARY KEY (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Preferred original languages, ISO 639-1 codes as TMDB uses them ('en', 'ja').
CREATE TABLE user_preferred_languages (
    user_id       BIGINT  NOT NULL,
    language_code CHAR(2) NOT NULL,
    CONSTRAINT pk_user_preferred_languages PRIMARY KEY (user_id, language_code),
    CONSTRAINT fk_user_preferred_languages_prefs FOREIGN KEY (user_id)
        REFERENCES user_preferences (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Preferred genres. TMDB genre IDs differ between movies and TV, so the media type is part of the key.
-- No foreign key to a genres table: genre IDs are TMDB data and are validated in the app.
CREATE TABLE user_preferred_genres (
    user_id    BIGINT      NOT NULL,
    media_type VARCHAR(10) NOT NULL,
    genre_id   INT         NOT NULL,
    CONSTRAINT pk_user_preferred_genres PRIMARY KEY (user_id, media_type, genre_id),
    CONSTRAINT fk_user_preferred_genres_prefs FOREIGN KEY (user_id)
        REFERENCES user_preferences (user_id) ON DELETE CASCADE,
    CONSTRAINT ck_user_preferred_genres_media_type CHECK (media_type IN ('MOVIE', 'TV'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
