package hudson.plugins.claim;

import hudson.plugins.claim.ClaimTestDataPublisher.Data;
import hudson.tasks.junit.TestAction;

public class ClaimTestAction extends AbstractClaimBuildAction<Data> {

	private String testObjectId;

	ClaimTestAction(Data owner, String testObjectId) {
		super(owner);
		this.testObjectId = testObjectId;
	}

	public String getDisplayName() {
		return "Claim Test";
	}
	
	@Override
	public void claim(String claimedBy, String reason, boolean sticky) {
		super.claim(claimedBy, reason, sticky);
		owner.addClaim(testObjectId, this);
	}
	
	@Override
	public String getNoun() {
		return "test";
	}

}
