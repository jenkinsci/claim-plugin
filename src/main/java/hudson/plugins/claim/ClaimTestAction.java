package hudson.plugins.claim;

import hudson.Util;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.claim.ClaimTestDataPublisher.Data;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResult;

import jakarta.mail.MessagingException;
import jenkins.model.Jenkins;
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

    // jelly
    public boolean isColumnDisplayed() {
        return !this.isUserAnonymous() && this.data.isDisplayClaimActionsInTestResultsTable();
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

    /**
     * Constructs the URL of the test result relative to the Jenkins instance.
     *
     * @return the relative URL of the test result.
     */
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
     * Gets the Jenkins base URL.
     *
     * @return the base URL of the Jenkins instance.
     */
    private String getJenkinsBaseUrl() {
        Jenkins instance = Jenkins.getInstance();
        if (instance != null) {
            return instance.getRootUrl();
        }
        return "";
    }

    /**
     * Constructs the absolute URL of the test result.
     *
     * @return the absolute URL of the test result.
     */
    @SuppressWarnings("unused")
    public String getAbsoluteUrl() {
        String baseUrl = getJenkinsBaseUrl();
        String jobUrl = data.getUrl() + "testReport/" + (this.testObjectId.startsWith("junit/") ? this.testObjectId.substring(6) : this.testObjectId);
        return Util.rawEncode(baseUrl + jobUrl + '/');
    }
}
