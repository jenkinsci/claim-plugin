package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.RootAction;

@Extension
public class ClaimedBuildsReport extends AbstractAssignedClaimsReport implements RootAction {

    public ClaimedBuildsReport() {
    }

    public String getDisplayName() {
        return Messages.ClaimedBuildsReport_DisplayName();
    }
}
