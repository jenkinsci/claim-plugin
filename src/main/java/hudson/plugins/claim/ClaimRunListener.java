package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * RunListener looking at broken builds with the {@link AllowBrokenBuildClaimingJobProperty} property to add them a ClaimAction.
 */
@Extension
public class ClaimRunListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    @Override
    public void onCompleted(Run<?, ?> r, TaskListener listener) {
        if (isBrokenBuild(r) && isPipelinePluginAvailable()) {
            Job<?, ?> job = r.getParent();
            if (r.getAction(ClaimBuildAction.class) == null && AllowBrokenBuildClaimingJobProperty.isClaimable(job)) {
                try {
                    ClaimPublisher.addClaimBuildAction(r);
                }
                catch (IOException e) {
                    LOGGER.warning("Exception adding claim to failed claimable build. Ignoring");
                }
            }
            
        }
    }

    private boolean isBrokenBuild(Run<?, ?> r) {
        if (r == null) {
            return false;
        }
        Result result = r.getResult();
        return result != null && result.isWorseThan(Result.SUCCESS);
    }

    private boolean isPipelinePluginAvailable() {
        return Jenkins.get().getPlugin("workflow-job") != null;
    }
}
