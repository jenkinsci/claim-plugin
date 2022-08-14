package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.kohsuke.stapler.DataBoundConstructor;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClaimPublisher extends Notifier implements SimpleBuildStep {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    @SuppressWarnings("unused")
    public static void initIcons() throws Exception {
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-sm",
                        "plugin/claim/images/claim.svg",
                        Icon.ICON_SMALL_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-md",
                        "plugin/claim/images/claim.svg",
                        Icon.ICON_MEDIUM_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-lg",
                        "plugin/claim/images/claim.svg",
                        Icon.ICON_LARGE_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-xlg",
                        "plugin/claim/images/claim.svg",
                        Icon.ICON_XLARGE_STYLE));
    }

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
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@NonNull Run<?, ?> build, @NonNull FilePath workspace, @NonNull Launcher launcher,
                        @NonNull TaskListener listener) throws InterruptedException, IOException {

        Result runResult = build.getResult();
        if (runResult != null && runResult.isWorseThan(Result.SUCCESS)) {
            addClaimBuildAction(build);
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
