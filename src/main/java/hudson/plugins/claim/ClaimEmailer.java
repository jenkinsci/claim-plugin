package hudson.plugins.claim;

import hudson.model.User;
import hudson.tasks.Mailer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import jenkins.model.JenkinsLocationConfiguration;
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
     * @param assignee the user assigned the failed build
     * @param assignedBy the user assigning the build
     * @param build the build/action which has been assigned
     * @param reason the reason given for the assignment
     * @param url the URL the user can view for the assigned build
     * @throws MessagingException if there has been some problem with sending the email
     * @throws IOException if there is an IO problem when sending the mail
     * @throws InterruptedException if the send operation is interrupted
     */
    public static void sendEmailIfConfigured(User assignee, String assignedBy, String build, String reason, String url)
            throws MessagingException, IOException, InterruptedException {

        ClaimConfig config = ClaimConfig.get();
        if (config.getSendEmails() && MAILER_LOADED) {
            MimeMessage msg = createMessage(assignee, assignedBy, build, reason, url);
            if (msg != null) {
                Transport.send(msg);
            }
        }
    }

    private static MimeMessage createMessage(User assignee, String assignedBy, String build, String reason, String url)
            throws MessagingException, IOException, InterruptedException {

        // create Session
        final Mailer.DescriptorImpl mailDescriptor = new Mailer.DescriptorImpl();
        MimeMessage msg = createMimeMessage(mailDescriptor);

        msg.setSentDate(new Date());
        msg.setSubject(Messages.ClaimEmailer_Subject(build), mailDescriptor.getCharset());
        //TODO configurable formatting, through email-ext plugin
        final String text = Messages.ClaimEmailer_Text(build, assignedBy)
                + System.getProperty("line.separator") + Messages.ClaimEmailer_Reason(reason)
                + System.getProperty("line.separator") + System.getProperty("line.separator")
                + Messages.ClaimEmailer_Details(getJenkinsLocationConfiguration().getUrl() + url);

        msg.setText(text, mailDescriptor.getCharset());
        Address userEmail = getUserEmail(assignee, mailDescriptor);
        if (userEmail == null) {
            return null;
        }
        msg.setRecipient(RecipientType.TO, userEmail);

        return msg;
    }

    /**
     * Creates MimeMessage using the mailer plugin for jenkins.
     *
     * @param mailDescriptor a reference to the mailer plugin from which we can get mailing parameters
     * @return mimemessage a message which can be emailed
     * @throws MessagingException if there has been some problem with sending the email
     * @throws UnsupportedEncodingException if an address provided is not in a correct format
     */
    private static MimeMessage createMimeMessage(final Mailer.DescriptorImpl mailDescriptor)
            throws UnsupportedEncodingException, MessagingException {
        MimeMessage ret = new MimeMessage(mailDescriptor.createSession());
        ret.setFrom(Mailer.stringToAddress(getJenkinsLocationConfiguration().getAdminAddress(),
                mailDescriptor.getCharset()));
        return ret;
    }

    /**
     * Returns the email address of a given user.
     *
     * @param user the user
     * @param mailDescriptor the descriptor allowing us to access mail config
     * @return email address for this user, null if none can be derived
     */
    private static Address getUserEmail(User user, Mailer.DescriptorImpl mailDescriptor) {
        if (user != null) {
            try {
                final Mailer.UserProperty mailProperty = user
                        .getProperty(Mailer.UserProperty.class);
                if (mailProperty != null && mailProperty.getAddress() != null) {
                    return new InternetAddress(mailProperty.getAddress());
                }
                return new InternetAddress(user.getId() + mailDescriptor.getDefaultSuffix());
            } catch (AddressException e) {
                LOGGER.log(Level.WARNING, "Cannot get email address for user " + user.getId(), e);
            }
        }
        return null;
    }

    @Nonnull
    private static JenkinsLocationConfiguration getJenkinsLocationConfiguration() {
        final JenkinsLocationConfiguration jlc = JenkinsLocationConfiguration.get();
        if (jlc == null) {
            throw new IllegalStateException("JenkinsLocationConfiguration not available");
        }
        return jlc;
    }
}
