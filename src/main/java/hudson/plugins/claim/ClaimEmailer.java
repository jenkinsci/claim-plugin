package hudson.plugins.claim;

import hudson.model.User;
import hudson.plugins.claim.messages.InitialBuildClaimMessage;
import hudson.plugins.claim.messages.InitialTestClaimMessage;
import hudson.plugins.claim.messages.RepeatedBuildClaimMessage;
import hudson.plugins.claim.messages.RepeatedTestClaimMessage;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;

import jakarta.mail.MessagingException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Email utility class to allow sending of emails using the setup of the mailer plug-in to do so.
 * If the mailer plug-in is not installed, then no emails are sent
 */
public final class ClaimEmailer {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    private static final boolean MAILER_LOADED = isMailerLoaded();

    private static boolean isMailerLoaded() {
        boolean ret = true;
        try {
            new Mailer.DescriptorImpl();
        } catch (Throwable e) {
            LOGGER.warning(
                    "Mailer plugin is not installed. Mailer plugin must be installed if you want to send emails");
            ret = false;
        }
        return ret;
    }

    private ClaimEmailer() {
        // makes no sense
    }

    /**
     * Sends an email to the assignee indicating that the given build has been assigned.
     * @param claimedByUser the claiming user
     * @param assignedByUser the assigner user
     * @param action the build/action which has been assigned
     * @param reason the reason given for the assignment
     * @param url the URL the user can view for the assigned build
     * @throws MessagingException if there has been some problem with sending the email
     * @throws IOException if there is an IO problem when sending the mail
     */
    public static void sendInitialBuildClaimEmailIfConfigured(@Nonnull User claimedByUser, @Nonnull User assignedByUser,
                                                              String action, String reason, String url)
            throws MessagingException, IOException {

        if (!isMailEnabledByUserPreference(claimedByUser, ClaimEmailPreference::isReceiveInitialBuildClaimEmail)) {
            LOGGER.log(java.util.logging.Level.FINE, "Initial build claim emails not configured for user {0}", claimedByUser.getDisplayName());
            return;
        }

        ClaimConfig config = ClaimConfig.get();
        if (config.getSendEmails() && MAILER_LOADED) {
            InitialBuildClaimMessage message = new InitialBuildClaimMessage(
                    action, url, reason, claimedByUser.getDisplayName(), assignedByUser.getDisplayName()
                );
            message.send();
        }
    }

    /**
     * Sends an email to the assignee indicating that the given test has been assigned.
     * @param claimedByUser the claiming user
     * @param assignedByUser the assigner user
     * @param action the build/action which has been assigned
     * @param reason the reason given for the assignment
     * @param url the URL the user can view for the assigned build
     * @throws MessagingException if there has been some problem with sending the email
     * @throws IOException if there is an IO problem when sending the mail
     */
    public static void sendInitialTestClaimEmailIfConfigured(@Nonnull User claimedByUser, @Nonnull User assignedByUser,
                                                             String action, String reason, String url)
        throws MessagingException, IOException {

        if (!isMailEnabledByUserPreference(claimedByUser, ClaimEmailPreference::isReceiveInitialTestClaimEmail)) {
            LOGGER.log(java.util.logging.Level.FINE, "Initial test claim emails not configured for user {0}", claimedByUser.getDisplayName());
            return;
        }

        ClaimConfig config = ClaimConfig.get();
        if (config.getSendEmails() && MAILER_LOADED) {
            InitialTestClaimMessage message = new InitialTestClaimMessage(
                    action, url, reason, claimedByUser.getDisplayName(), assignedByUser.getDisplayName()
                );
            message.send();
        }
    }

    /**
     * Sends an email to the assignee indicating that the given build is still failing.
     * @param claimedByUser the claiming user
     * @param action the build/action which has been assigned
     * @param url the URL the user can view for the assigned build
     * @throws MessagingException if there has been some problem with sending the email
     * @throws IOException if there is an IO problem when sending the mail
     */
    public static void sendRepeatedBuildClaimEmailIfConfigured(@Nonnull User claimedByUser, String action, String url)
        throws MessagingException, IOException {

        if (!isMailEnabledByUserPreference(claimedByUser, ClaimEmailPreference::isReceiveRepeatedBuildClaimEmail)) {
            LOGGER.log(java.util.logging.Level.FINE, "Repeated Build claim emails not configured for user {0}", claimedByUser.getDisplayName());
            return;
        }

        ClaimConfig config = ClaimConfig.get();
        if (config.getSendEmailsForStickyFailures() && MAILER_LOADED) {
            RepeatedBuildClaimMessage message = new RepeatedBuildClaimMessage(action, url, claimedByUser.getDisplayName());
            message.send();
        }
    }

    /**
     * Sends an email to the assignee indicating that the given tests are still failing.
     * @param claimedByUser name of the claiming user
     * @param action the build/action which has been assigned
     * @param url the URL the user can view for the assigned build
     * @param failedTests the list of failed tests
     * @throws MessagingException if there has been some problem with sending the email
     * @throws IOException if there is an IO problem when sending the mail
     */
    public static void sendRepeatedTestClaimEmailIfConfigured(@Nonnull User claimedByUser, String action, String url,
                                                              List<CaseResult> failedTests)
        throws MessagingException, IOException {

        if (!isMailEnabledByUserPreference(claimedByUser, ClaimEmailPreference::isReceiveRepeatedTestClaimEmail)) {
            LOGGER.log(java.util.logging.Level.FINE, "Repeated test claim emails not configured for user {0}", claimedByUser.getDisplayName());
            return;
        }

        ClaimConfig config = ClaimConfig.get();
        if (config.getSendEmailsForStickyFailures() && MAILER_LOADED) {
            RepeatedTestClaimMessage message = new RepeatedTestClaimMessage(action, url, claimedByUser.getDisplayName(), failedTests);
            message.send();
        }
    }

    @FunctionalInterface
    private interface EmailPreferenceChecker {
        boolean shouldSend(ClaimEmailPreference preference);
    }

    private static boolean isMailEnabledByUserPreference(@Nonnull User user, EmailPreferenceChecker preferenceChecker) {
        ClaimEmailPreference preference = user.getProperty(ClaimEmailPreference.class);
        if (preference == null) {
            // Default: Send mails
            return true;
        }

        return preferenceChecker.shouldSend(preference);
    }
}
