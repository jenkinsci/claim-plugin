package hudson.plugins.claim;

import hudson.plugins.claim.ClaimTestDataPublisher.Data;

public class ClaimTestAction extends AbstractClaimBuildAction<Data> {

    private String testObjectId;

    ClaimTestAction(Data owner, String testObjectId) {
        super(owner);
        this.testObjectId = testObjectId;
    }

    public String getDisplayName() {
        return Messages.ClaimTestAction_DisplayName();
    }

    @Override
    public void claim(String claimedBy, String reason, String assignedBy, boolean sticky) {
        super.claim(claimedBy, reason, assignedBy, sticky);
        owner.addClaim(testObjectId, this);
    }

    @Override
    public String getNoun() {
        return Messages.ClaimTestAction_Noun();
    }

    @Override
    String getUrl() {
        return owner.getURL();
    }

}
