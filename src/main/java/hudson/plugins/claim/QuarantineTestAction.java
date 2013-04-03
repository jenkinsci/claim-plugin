package hudson.plugins.claim;

import hudson.plugins.claim.ClaimTestDataPublisher.Data;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.ProminentProjectAction;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.tasks.junit.TestAction;

public class QuarantineTestAction 
	extends TestAction 
	implements BuildBadgeAction, ProminentProjectAction
{

	
	QuarantineTestAction(Data owner, String testObjectId) {
	}

	public String getDisplayName() {
		return Messages.QuarantineTestAction_DisplayName();
	}
	
	public String getIconFileName() {
		return null;
	}

	public String getUrlName() {
		return "quarantine";
	}	
	
//	@Override
//	public void claim(String claimedBy, String reason, boolean sticky) {
//		super.claim(claimedBy, reason, sticky);
//		owner.addClaim(testObjectId, this);
//	}
	
	public String getNoun() {
		return Messages.QuarantineTestAction_Noun();
	}

}
