package hudson.plugins.claim.messages;

import hudson.plugins.claim.Messages;
import hudson.tasks.junit.CaseResult;

import java.util.List;
import java.util.stream.Collectors;

public final class RepeatedTestClaimMessage extends RepeatedClaimMessage {
    private final List<CaseResult> failedTests;

    public RepeatedTestClaimMessage(String item, String url, String claimedByUser, List<CaseResult> failedTests) {
        super(item, url, claimedByUser);
        this.failedTests = failedTests;
    }

    @Override
    protected String getSpecificMessage() {
        String testsDetails = failedTests.stream()
            .map(it -> Messages.ClaimEmailer_Test_Repeated_Details(it.getFullDisplayName()))
            .collect(Collectors.joining(LINE_SEPARATOR));

        return Messages.ClaimEmailer_Test_Repeated_Text(failedTests.size(), getItem())
            + LINE_SEPARATOR
            + LINE_SEPARATOR
            + testsDetails;
    }

    @Override
    protected boolean mustBeSent() {
        return !failedTests.isEmpty();
    }

    @Override
    protected String getSubject() {
        return Messages.ClaimEmailer_Test_Repeated_Subject(failedTests.size(), getItem());
    }
}
