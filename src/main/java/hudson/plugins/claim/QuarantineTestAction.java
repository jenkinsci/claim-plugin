package hudson.plugins.claim;

import hudson.plugins.claim.QuarantineTestDataPublisher.Data;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.ProminentProjectAction;
import hudson.model.User;
import hudson.tasks.junit.TestAction;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import javax.servlet.ServletException;

@ExportedBean(defaultVisibility = 2)
public class QuarantineTestAction 
	extends TestAction 
	implements BuildBadgeAction, ProminentProjectAction
{
	private boolean quarantined;
	private String quarantinedBy;
	private Date quarantineDate;
	private String reason;
	private String testObjectId;
    	
	protected Data owner;
	
	QuarantineTestAction(Data owner, String testObjectId) {
		this.owner = owner;
		this.testObjectId = testObjectId;
	}

	public void doQuarantine(StaplerRequest req, StaplerResponse resp)
		throws ServletException, IOException {
		Authentication authentication = Hudson.getAuthentication();
		String name = authentication.getName();
		String reason = (String) req.getSubmittedForm().get("reason");
		if (StringUtils.isEmpty(reason)) reason = null;
		quarantine(name, reason);
		owner.save();
		resp.forwardToPreviousPage(req);
	}

	public void doRelease(StaplerRequest req, StaplerResponse resp)
	throws ServletException, IOException {
		release();
		owner.save();
		resp.forwardToPreviousPage(req);
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
	
	public String quarantinedByName()
	{
		User user = User.get(quarantinedBy, false, Collections.emptyMap());
		if (user != null) {
			return user.getDisplayName();
		} else {
			return quarantinedBy;
		}
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public Date getDate()
	{
		return quarantineDate;
	}
	
	public boolean hasReason() {
		return !StringUtils.isEmpty(reason);
	}
	
	public String getLatestResultUrl()
	{
		String url = "";
		hudson.tasks.test.TestResult tr = owner.getResultForTestId(testObjectId);
		
		if (tr != null)
		{
			url = tr.getOwner().getParent().getUrl() + "lastCompletedBuild/testReport" + tr.getUrl();
		}
		
		return url;
	}
	
	/**
	 * Whether this is the latest result. As it does not make sense to enable
	 * quarantining on an old result, use this to not even display the option to quarantine it
	 */
	public boolean isLatestResult()
	{
		return owner.isLatestResult();
	}
	
	public boolean isUserAnonymous() {
		return Hudson.getAuthentication().getName().equals("anonymous");
	}

	public void quarantine(String quarantinedBy, String reason, Date date)
	{
		this.quarantined = true;
		this.quarantinedBy = quarantinedBy;
		this.reason = reason;
		this.quarantineDate = date;
		owner.addQuarantine(testObjectId, this);		
	}
	
	public void quarantine(String quarantinedBy, String reason) {
		quarantine(quarantinedBy,reason,new Date());
	}
	
	public void quarantine(QuarantineTestAction action) {
		quarantine(action.quarantinedByName(),action.getReason(),action.getDate());
	}
	
	public void release() {
		this.quarantined = false;
		this.quarantinedBy = null;
		this.quarantineDate = null;
		// we remember the reason to show it if someone puts this test back in quarantine.
	}

	@Override
	public String toString() {
		return "QuarantineTestAction(quarantined="+quarantined+",quarantinedBy="+quarantinedBy+",reason="+reason+")";
	}

}
