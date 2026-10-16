package com.watchvault.library;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Add a title to the library, or change its entry (PUT: the whole entry is replaced). */
public record SaveLibraryEntryRequest(

		@NotNull(message = "Status is required.")
		LibraryStatus status,

		@Min(value = LibraryEntry.MIN_RATING, message = "Rating must be from 1 to 10.")
		@Max(value = LibraryEntry.MAX_RATING, message = "Rating must be from 1 to 10.")
		Integer rating,

		@Size(max = LibraryEntry.MAX_NOTES_LENGTH, message = "Notes can be at most 1000 characters.")
		// Normal text, line breaks and tabs only: no other control characters.
		@Pattern(regexp = "(?:[^\\p{Cntrl}]|[\\r\\n\\t])*", message = "Notes contain characters that are not allowed.")
		String notes) {

	/** Trim notes; blank notes are stored as "no notes". */
	public SaveLibraryEntryRequest {
		if (notes != null) {
			notes = notes.strip();
			if (notes.isEmpty()) {
				notes = null;
			}
		}
	}
}
