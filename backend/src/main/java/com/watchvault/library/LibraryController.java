package com.watchvault.library;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.watchvault.auth.CurrentUser;
import com.watchvault.media.MediaTypePaths;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * The logged-in user's library. Entries are addressed by title (movie/123), never by a database
 * id, and the owner always comes from the token: there is nothing in the URL to point at
 * someone else's data.
 */
@RestController
@RequestMapping("/api/library")
public class LibraryController {

	private final LibraryService library;

	public LibraryController(LibraryService library) {
		this.library = library;
	}

	@GetMapping
	public List<LibraryEntryResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return library.list(CurrentUser.id(jwt));
	}

	@GetMapping("/{mediaType}/{tmdbId}")
	public LibraryEntryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String mediaType,
			@PathVariable @Positive int tmdbId) {
		return library.get(CurrentUser.id(jwt), MediaTypePaths.fromPath(mediaType), tmdbId);
	}

	@PutMapping("/{mediaType}/{tmdbId}")
	public LibraryEntryResponse save(@AuthenticationPrincipal Jwt jwt, @PathVariable String mediaType,
			@PathVariable @Positive int tmdbId, @Valid @RequestBody SaveLibraryEntryRequest request) {
		return library.save(CurrentUser.id(jwt), MediaTypePaths.fromPath(mediaType), tmdbId, request);
	}

	@DeleteMapping("/{mediaType}/{tmdbId}")
	public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String mediaType,
			@PathVariable @Positive int tmdbId) {
		library.remove(CurrentUser.id(jwt), MediaTypePaths.fromPath(mediaType), tmdbId);
		return ResponseEntity.noContent().build();
	}
}
