package hudson.plugins.claim;

import hudson.ExtensionPoint;
import hudson.ExtensionList;
import hudson.model.Hudson;

public abstract class ClaimListener implements ExtensionPoint {

	public static ExtensionList<ClaimListener> all() {
		return Hudson.getInstance().getExtensionList(ClaimListener.class);
	}

	public void claimed(AbstractClaimBuildAction claim) {
	}

	public void unclaimed(AbstractClaimBuildAction claim) {}

}
