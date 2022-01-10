package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.Job;
import jenkins.model.OptionalJobProperty;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * {@link OptionalJobProperty} to allow broken builds to be "claimable".
 */
@ExportedBean
public class AllowBrokenBuildClaimingJobProperty extends OptionalJobProperty<WorkflowJob> {

    @DataBoundConstructor
    public AllowBrokenBuildClaimingJobProperty() {
    }

    @Extension(optional = true)
    @Symbol("allowBrokenBuildClaiming")
    public static class DescriptorImpl extends OptionalJobPropertyDescriptor {

        @Override public String getDisplayName() {
            return Messages.ClaimPublisher_DisplayName();
        }

    }

    public static boolean isClaimable(Job<?, ?> job) {
        return job.getProperty(AllowBrokenBuildClaimingJobProperty.class) != null;
    }

}