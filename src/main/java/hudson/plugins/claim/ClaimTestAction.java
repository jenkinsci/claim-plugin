package hudson.plugins.claim;

import hudson.model.Run;
import hudson.plugins.claim.ClaimTestDataPublisher.Data;

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
    public void claim(String claimedBy, String reason, String assignedBy, boolean sticky) {
        super.claim(claimedBy, reason, assignedBy, sticky);
        data.addClaim(testObjectId, this);
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
