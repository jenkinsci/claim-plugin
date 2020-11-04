package hudson.plugins.claim;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MessagesTest {

	@Test
	public void msgTestsRepeatedSubject() {
		assertEquals("1 assigned test still failing for foo", Messages.ClaimEmailer_Tests_Repeated_Subject(1, "foo"));
		assertEquals("2 assigned tests still failing for foo", Messages.ClaimEmailer_Tests_Repeated_Subject(2, "foo"));
		assertEquals("5 assigned tests still failing for foo", Messages.ClaimEmailer_Tests_Repeated_Subject(5, "foo"));
	}

	@Test
	public void msgTestsRepeatedText() {
		assertEquals("A test assigned to you is still failing in foo:", Messages.ClaimEmailer_Tests_Repeated_Text(1, "foo"));
		assertEquals("2 tests assigned to you are still failing in foo:", Messages.ClaimEmailer_Tests_Repeated_Text(2, "foo"));
		assertEquals("5 tests assigned to you are still failing in foo:", Messages.ClaimEmailer_Tests_Repeated_Text(5, "foo"));
	}
}
