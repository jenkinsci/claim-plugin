package hudson.plugins.claim;

import hudson.plugins.claim.QuarantineTestDataPublisher.Data;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.ProminentProjectAction;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.tasks.junit.TestAction;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import java.util.Date;

@ExportedBean(defaultVisibility = 2)
public class QuarantineTestAction 
	extends TestAction 
	implements BuildBadgeAction, ProminentProjectAction
{
	private boolean quarantined;
	private String quarantinedBy;
	private Date quarantineDate;
	private String reason;
    	
	protected Data owner;
	
	QuarantineTestAction(Data owner, String testObjectId) {
		this.owner = owner;
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
	
	public String getNoun() {
		return Messages.QuarantineTestAction_Noun();
	}
	
	@Exported
	public boolean isQuarantined() {
		return quarantined;
	}

	public void quarantine(String quarantinedBy, String reason) {
		this.quarantined = true;
		this.quarantinedBy = quarantinedBy;
		this.reason = reason;
		this.quarantineDate = new Date();
	}

}
