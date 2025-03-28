package hudson.plugins.claim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {

    @Test
    void msgTestsRepeatedSubject() {
        assertEquals("1 assigned test still failing for foo",
            Messages.ClaimEmailer_Test_Repeated_Subject(1, "foo"));
        assertEquals("2 assigned tests still failing for foo",
            Messages.ClaimEmailer_Test_Repeated_Subject(2, "foo"));
        assertEquals("5 assigned tests still failing for foo",
            Messages.ClaimEmailer_Test_Repeated_Subject(5, "foo"));
    }

    @Test
    void msgTestsRepeatedText() {
        assertEquals("A test assigned to you is still failing in foo:",
            Messages.ClaimEmailer_Test_Repeated_Text(1, "foo"));
        assertEquals("2 tests assigned to you are still failing in foo:",
            Messages.ClaimEmailer_Test_Repeated_Text(2, "foo"));
        assertEquals("5 tests assigned to you are still failing in foo:",
            Messages.ClaimEmailer_Test_Repeated_Text(5, "foo"));
    }
}
