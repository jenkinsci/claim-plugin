package hudson.plugins.claim;

import hudson.model.Run;

public class ClaimBuildAction extends AbstractClaimBuildAction<Run> {

	private static final long serialVersionUID = 1L;

	public ClaimBuildAction(Run run) {
		super(run);
	}

	public String getDisplayName() {
		return "Claim Build";
	}
	
	@Override
	public String getNoun() {
		return "build";
	}

}
