package hudson.plugins.claim;

import hudson.model.Run;

final class ClaimUtils {

    private ClaimUtils() {
    }

    public static ClaimBuildAction getBuildAction(Run run, boolean onlyIfClaimed) {
        ClaimBuildAction action = run.getAction(ClaimBuildAction.class);
        if (onlyIfClaimed && (action == null || !action.isClaimed())) {
            return null;
        }
        return action;
    }
}
