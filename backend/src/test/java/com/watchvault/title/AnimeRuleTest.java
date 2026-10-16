package com.watchvault.title;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class AnimeRuleTest {

	private static final int ACTION = 28;

	@Test
	void japaneseAnimationIsAnime() {
		assertThat(AnimeRule.isAnime(List.of(ACTION, AnimeRule.ANIMATION_GENRE_ID), "ja")).isTrue();
	}

	@Test
	void animationInAnotherLanguageIsNotAnime() {
		assertThat(AnimeRule.isAnime(List.of(AnimeRule.ANIMATION_GENRE_ID), "en")).isFalse();
	}

	@Test
	void japaneseLiveActionIsNotAnime() {
		assertThat(AnimeRule.isAnime(List.of(ACTION), "ja")).isFalse();
	}

	@Test
	void unknownLanguageIsNotAnime() {
		assertThat(AnimeRule.isAnime(List.of(AnimeRule.ANIMATION_GENRE_ID), null)).isFalse();
	}
}
