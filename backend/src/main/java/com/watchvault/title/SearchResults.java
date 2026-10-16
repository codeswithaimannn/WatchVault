package com.watchvault.title;

import java.util.List;

/** One page of search results. Only movies and TV shows; people are left out. */
public record SearchResults(int page, int totalPages, List<TitleSummary> results) {
}
