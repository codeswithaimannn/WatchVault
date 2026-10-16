package com.watchvault.library;

/** Must match the CHECK constraint on library_entries.status (V4 migration). */
public enum LibraryStatus {
	PLAN_TO_WATCH,
	WATCHING,
	COMPLETED,
	ON_HOLD,
	DROPPED
}
