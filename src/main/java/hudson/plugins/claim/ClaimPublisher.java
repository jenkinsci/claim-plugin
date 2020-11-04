package hudson.plugins.claim;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;

import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.kohsuke.stapler.DataBoundConstructor;

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

public final class ClaimPublisher extends Notifier implements SimpleBuildStep {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    @SuppressWarnings("unused")
    public static void initIcons() throws Exception {
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-sm",
                        "plugin/claim/images/16x16/claim.png",
                        Icon.ICON_SMALL_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-md",
                        "plugin/claim/images/24x24/claim.png",
                        Icon.ICON_MEDIUM_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-lg",
                        "plugin/claim/images/32x32/claim.png",
                        Icon.ICON_LARGE_STYLE));
        IconSet.icons.addIcon(
                new Icon("icon-claim-claim icon-xlg",
                        "plugin/claim/images/48x48/claim.png",
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

        @Nonnull
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
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {

        Result runResult = build.getResult();
        if (runResult != null && runResult.isWorseThan(Result.SUCCESS)) {
            ClaimBuildAction action = new ClaimBuildAction();
            build.addAction(action);
            build.save();

            // check if previous build was claimed
            Run<?, ?> previousBuild = build.getPreviousBuild();
            if (previousBuild != null) {
                ClaimBuildAction c = previousBuild.getAction(ClaimBuildAction.class);
                if (c != null && c.isClaimed() && c.isSticky()) {
                    c.copyTo(action);
                    sendEmailsForStickyFailureIfConfigured(build, c.getClaimedBy());
                }
            }
        }

    }

    private void sendEmailsForStickyFailureIfConfigured(Run<?, ?> build, String claimedBy) {
        if (ClaimConfig.get().isSendEmailsForStickyFailures()) {
        	try {
				ClaimEmailer.sendEmailForStickyClaim(build, claimedBy);
			} catch (MessagingException | IOException | InterruptedException e) {
	            LOGGER.log(Level.WARNING, "Exception when sending build failure reminder email. Ignoring.", e);
			}
        }
	}

	public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

}
