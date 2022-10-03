package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClaimPublisher extends Notifier implements SimpleBuildStep {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    @DataBoundConstructor
    public ClaimPublisher() {
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public String getHelpFile() {
            return "/plugin/claim/help.html";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.ClaimPublisher_DisplayName();
        }

        public boolean isApplicable(Class jobType) {
            return true;
        }

    }

    @Override
    public boolean requiresWorkspace() {
        return false;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener) throws InterruptedException, IOException {
        Result runResult = run.getResult();
        if (runResult != null && runResult.isWorseThan(Result.SUCCESS)) {
            addClaimBuildAction(run);
        }
    }

    static void addClaimBuildAction(Run<?, ?> build) throws IOException {
        ClaimBuildAction action = new ClaimBuildAction();
        build.addAction(action);
        build.save();

        // check if previous build was claimed
        Run<?, ?> previousBuild = build.getPreviousBuild();
        if (previousBuild != null) {
            ClaimBuildAction c = previousBuild.getAction(ClaimBuildAction.class);
            if (c != null && c.isClaimed() && c.isSticky()) {
                c.copyTo(action);
                sendEmailsForStickyFailure(build, c.getClaimedBy());
            }
        }
    }

    private static void sendEmailsForStickyFailure(Run<?, ?> build, String claimedByUser) {
        try {
            ClaimEmailer.sendRepeatedBuildClaimEmailIfConfigured(claimedByUser, build.toString(), build.getUrl());
        } catch (MessagingException | IOException e) {
            LOGGER.log(Level.WARNING, "Exception when sending build failure reminder email. Ignoring.", e);
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

}
