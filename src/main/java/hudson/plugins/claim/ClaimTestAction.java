package hudson.plugins.claim;

import hudson.model.Run;
import hudson.plugins.claim.ClaimTestDataPublisher.Data;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResult;

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
    protected void applyClaim(String claimedByUser, String providedReason, String assignedByUser, boolean isSticky,
                              boolean isPropagated) {
        data.addClaim(testObjectId, this);
        super.applyClaim(claimedByUser, providedReason, assignedByUser, isSticky, isPropagated);
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
        return data.getUrl();
    }

    @Override
    protected Run getOwner() {
        return data.getBuild();
    }
}
