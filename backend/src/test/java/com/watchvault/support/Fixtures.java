package com.watchvault.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Reads a test fixture file from src/test/resources/fixtures/. */
public final class Fixtures {

	private Fixtures() {
	}

	public static String read(String path) {
		try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + path)) {
			if (in == null) {
				throw new IllegalArgumentException("No fixture at /fixtures/" + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
