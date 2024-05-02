package hudson.plugins.claim;

import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.claim.ClaimTestDataPublisher.Data;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResult;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

public final class ClaimTestAction extends AbstractClaimBuildAction<Run> {

    private String testObjectId;
    private Data data;

    ClaimTestAction(Data data, String testObjectId) {
        this.data = data;
        this.testObjectId = testObjectId;
    }

    public String getDisplayName() {
        return Messages.ClaimTestAction_DisplayName();
    }

    @Override
    protected void applyClaim(User claimedByUser, String providedReason, User assignedByUser, Date date,
                              boolean isSticky, boolean isPropagated) {
        data.addClaim(testObjectId, this);
        super.applyClaim(claimedByUser, providedReason, assignedByUser, date, isSticky, isPropagated);
    }

    @Override
    protected Optional<AbstractClaimBuildAction> getNextAction() {
        Run nextRun = getOwner().getNextBuild();
        if (nextRun != null) {
            TestResultAction action = nextRun.getAction(TestResultAction.class);
            if (action != null) {
                TestResult testResult = action.getResult().findCorrespondingResult(testObjectId);
                if (testResult != null) {
                    ClaimTestAction claimAction = testResult.getTestAction(ClaimTestAction.class);
                    return Optional.ofNullable(claimAction);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String getNoun() {
        return Messages.ClaimTestAction_Noun();
    }

    @Override
    String getUrl() {
        return data.getUrl() + "testReport/" + this.testObjectId;
    }

    @Override
    protected Run getOwner() {
        return data.getBuild();
    }

    @Override
    protected void sendInitialClaimEmail(User claimedByUser, String providedReason, User assignedByUser)
        throws MessagingException, IOException {
    ClaimEmailer.sendInitialTestClaimEmailIfConfigured(
        claimedByUser,
        assignedByUser,
        getOwner().toString(),
        providedReason,
        getUrl());
    }

    /**
     * Gets the relative URL to this test action from the test report page.
     * If the test object ID starts with a 'junit/' prefix, it strips this prefix to maintain compatibility
     * with changes in test object ID formatting.
     *
     * @return the relative URL of the test object after adjusting for backward compatibility.
     */
    public String getRelativeUrlFromTestReportPage() {
        if (testObjectId.startsWith("junit")){
            return testObjectId.substring(6);
        }
        return testObjectId;
    }
}
