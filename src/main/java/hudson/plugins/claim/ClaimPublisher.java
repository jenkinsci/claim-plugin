package hudson.plugins.claim;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

public class ClaimPublisher extends Notifier {

    @DataBoundConstructor
    public ClaimPublisher() {
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public String getHelpFile() {
            return "/plugin/claim/help.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.ClaimPublisher_DisplayName();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {

        if (build.getResult().isWorseThan(Result.SUCCESS)) {
            ClaimBuildAction action = new ClaimBuildAction(build);
            build.addAction(action);
            build.save();

            // check if previous build was claimed
            AbstractBuild<?,?> previousBuild = build.getPreviousBuild();
            if (previousBuild != null) {
                ClaimBuildAction c = previousBuild.getAction(ClaimBuildAction.class);
                if (c != null && c.isClaimed() && c.isSticky()) {
                    c.copyTo(action);
                }
            }
        }

        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

}
