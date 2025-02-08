package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.UserProperty;
import org.kohsuke.stapler.DataBoundConstructor;

public class ClaimEmailPreference extends UserProperty {

    private final boolean receiveInitialBuildClaimEmail;
    private final boolean receiveInitialTestClaimEmail;
    private final boolean receiveRepeatedBuildClaimEmail;
    private final boolean receiveRepeatedTestClaimEmail;

    @DataBoundConstructor
    public ClaimEmailPreference(boolean receiveInitialBuildClaimEmail, boolean receiveInitialTestClaimEmail, boolean receiveRepeatedBuildClaimEmail, boolean receiveRepeatedTestClaimEmail) {
        this.receiveInitialBuildClaimEmail = receiveInitialBuildClaimEmail;
        this.receiveInitialTestClaimEmail = receiveInitialTestClaimEmail;
        this.receiveRepeatedBuildClaimEmail = receiveRepeatedBuildClaimEmail;
        this.receiveRepeatedTestClaimEmail = receiveRepeatedTestClaimEmail;
    }

    public boolean isReceiveInitialBuildClaimEmail() {
        return receiveInitialBuildClaimEmail;
    }

    public boolean isReceiveInitialTestClaimEmail() {
        return receiveInitialTestClaimEmail;
    }

    public boolean isReceiveRepeatedBuildClaimEmail() {
        return receiveRepeatedBuildClaimEmail;
    }

    public boolean isReceiveRepeatedTestClaimEmail() {
        return receiveRepeatedTestClaimEmail;
    }



    @Extension
    public static class DescriptorImpl extends hudson.model.UserPropertyDescriptor {
        @Override
        public UserProperty newInstance(hudson.model.User user) {
            return new ClaimEmailPreference(true, true, true, true);  // Default: keine E-Mails
        }

        @Override
        public String getDisplayName() {
            return "Claim E-Mail Preferences";
        }

        public boolean isAllowUsersToConfigureEmailPreferences() {
            var config = ClaimConfig.get();
            if (config == null) {
                return false;
            }
            return config.isAllowUsersToConfigureEmailPreferences();
        }

    }
}
