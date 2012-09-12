package jgloss.util;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Iterator;

import org.junit.Test;


public class StringToolsTest {
	private static final String BAR = "bar";
	private static final String FOO = "foo";

	@Test
	public void testTokenizeTwoWordsAllSeparators() {
		Iterable<String> tokenizeIterable = StringTools.tokenize("/foo/bar/", "/");
		assertThat(tokenizeIterable).isNotNull();
		Iterator<String> tokens = tokenizeIterable.iterator();
		assertThat(tokens).isNotNull();
		assertThat(tokens.hasNext()).isTrue();
		assertThat(tokens.next()).isEqualTo(FOO);
		assertThat(tokens.hasNext()).isTrue();
		assertThat(tokens.next()).isEqualTo(BAR);
		assertThat(tokens.hasNext()).isFalse();
	}
	
	@Test
	public void testTokenizeTwoWordsOneSeparator() {
		Iterable<String> tokenizeIterable = StringTools.tokenize("foo/bar", "/");
		assertThat(tokenizeIterable).isNotNull();
		Iterator<String> tokens = tokenizeIterable.iterator();
		assertThat(tokens).isNotNull();
		assertThat(tokens.hasNext()).isTrue();
		assertThat(tokens.next()).isEqualTo(FOO);
		assertThat(tokens.hasNext()).isTrue();
		assertThat(tokens.next()).isEqualTo(BAR);
		assertThat(tokens.hasNext()).isFalse();
	}
	
	@Test
	public void testTokenizeOneWordNoSeparator() {
		Iterable<String> tokenizeIterable = StringTools.tokenize(FOO, "/");
		assertThat(tokenizeIterable).isNotNull();
		Iterator<String> tokens = tokenizeIterable.iterator();
		assertThat(tokens).isNotNull();
		assertThat(tokens.hasNext()).isTrue();
		assertThat(tokens.next()).isEqualTo(FOO);
		assertThat(tokens.hasNext()).isFalse();
	}

	@Test
	public void testTokenizeOneSeparator() {
		Iterable<String> tokenizeIterable = StringTools.tokenize("/", "/");
		assertThat(tokenizeIterable).isNotNull();
		Iterator<String> tokens = tokenizeIterable.iterator();
		assertThat(tokens.hasNext()).isFalse();
	}
}
