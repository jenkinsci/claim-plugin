package hudson.plugins.claim;

import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;

import java.io.IOException;

import javax.servlet.ServletException;

import org.acegisecurity.Authentication;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ClaimBuildAction implements BuildBadgeAction,
		ProminentProjectAction {

	private static final long serialVersionUID = 1L;

	private String claimedBy;
	private boolean claimed;
	private final Run<?, ?> run;

	public ClaimBuildAction(Run run) {
		this.run = run;
	}

	@Override
	public String getDisplayName() {
		return "Claim Build";
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getUrlName() {
		return "claim";
	}

	public void doIndex(StaplerRequest req, StaplerResponse resp)
			throws ServletException, IOException {
		Authentication authentication = Hudson.getAuthentication();
		String name = authentication.getName();
		if (!claimed || !name.equals(claimedBy)) {
			claim(name);
		} else {
			unclaim();
		}
		run.save();
		resp.forwardToPreviousPage(req);
	}

	public String getClaimedBy() {
		return claimedBy;
	}

	public void setClaimedBy(String claimedBy) {
		this.claimedBy = claimedBy;
	}

	public boolean isClaimed() {
		return claimed;
	}

	public void claim(String claimedBy) {
		this.claimed = true;
		this.claimedBy = claimedBy;
	}

	public void unclaim() {
		this.claimed = false;
		this.claimedBy = null;
	}

	public boolean isClaimedByMe() {
		return !isUserAnonymous()
				&& Hudson.getAuthentication().getName().equals(claimedBy);
	}

	public boolean canClaim() {
		return !isUserAnonymous() && !isClaimedByMe();
	}

	public boolean canRelease() {
		return !isUserAnonymous() && isClaimedByMe();
	}

	public boolean isUserAnonymous() {
		return Hudson.getAuthentication().getName().equals("anonymous");
	}
}
