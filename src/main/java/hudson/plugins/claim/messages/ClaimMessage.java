package hudson.plugins.claim.messages;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.User;
import hudson.tasks.Mailer;
import jenkins.model.JenkinsLocationConfiguration;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO configurable formatting, through email-ext plugin
abstract class ClaimMessage {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");
    protected static final String LINE_SEPARATOR =  System.getProperty("line.separator");

    private String item;
    private String url;
    private String claimedByUser;

    ClaimMessage(String item, String url, String claimedByUser) {
        this.item = item;
        this.url = url;
        this.claimedByUser = claimedByUser;
    }

    /**
     * Gets the item.
     * @return the item
     */
    protected String getItem() {
        return item;
    }

    /**
     * Gets the claiming user.
     * @return the claiming user
     */
    protected String getClaimedByUser() {
        return claimedByUser;
    }

    /**
     * Creates and send the communication.
     * @throws MessagingException if there has been some problem with sending the email
     * @throws IOException if there has been some problem with the mail content
     */
    public void send() throws MessagingException, IOException {
        if (mustBeSent()) {
            MimeMessage message = createMessage();
            sendMessage(message);
        }
    }

    protected abstract boolean mustBeSent();

    protected abstract String getMessage();

    protected abstract String getSubject();

    protected abstract Iterable<? extends String> getToRecipients();

    /**
     * Get the jenkins url for the item.
     * @return The jenkins url for the item
     */
    @NonNull
    protected String buildJenkinsUrl() {
        return getJenkinsLocationConfiguration().getUrl() + url;
    }
    @NonNull
    private static JenkinsLocationConfiguration getJenkinsLocationConfiguration() {
        final JenkinsLocationConfiguration jlc = JenkinsLocationConfiguration.get();
        if (jlc == null) {
            throw new IllegalStateException("JenkinsLocationConfiguration not available");
        }
        return jlc;
    }

    /**
     * Creates MimeMessage and sets its content and recipients.
     * @return message which can be emailed
     * @throws MessagingException if there has been some problem with sending the email
     * @throws UnsupportedEncodingException if an address provided is not in a correct format
     */
    @NonNull
    private MimeMessage createMessage()
        throws MessagingException, UnsupportedEncodingException {

        // create Session
        final Mailer.DescriptorImpl mailDescriptor = new Mailer.DescriptorImpl();
        MimeMessage msg = createMimeMessage(mailDescriptor);

        msg.setSentDate(new Date());
        msg.setSubject(getSubject(), mailDescriptor.getCharset());
        msg.setText(getMessage());
        for (String to: getToRecipients()) {
            Address userEmail = getUserEmail(to, mailDescriptor);
            if (userEmail != null) {
                msg.setRecipient(Message.RecipientType.TO, userEmail);
            }
        }
        return msg;
    }

    /**
     * Creates MimeMessage using the mailer plugin for jenkins.
     *
     * @param mailDescriptor a reference to the mailer plugin from which we can get mailing parameters
     * @return a message which can be emailed
     * @throws MessagingException if there has been some problem with sending the email
     * @throws UnsupportedEncodingException if an address provided is not in a correct format
     */
    @NonNull
    private static MimeMessage createMimeMessage(final Mailer.DescriptorImpl mailDescriptor)
        throws MessagingException, UnsupportedEncodingException {

        MimeMessage msg = new MimeMessage(mailDescriptor.createSession());
        msg.setFrom(
                Mailer.stringToAddress(getJenkinsLocationConfiguration().getAdminAddress(),
                mailDescriptor.getCharset())
            );
        return msg;
    }

    private static void sendMessage(MimeMessage msg) throws MessagingException {
        Address[] recipients = msg.getAllRecipients();
        if (recipients != null && recipients.length > 0) {
            Transport.send(msg);
        }
    }

    /**
     * Returns the email address of a given user.
     *
     * @param claimedByUser name of the claiming user
     * @param mailDescriptor the descriptor allowing us to access mail config
     * @return email address for this user, null if none can be derived
     */
    @CheckForNull
    private static Address getUserEmail(String claimedByUser, Mailer.DescriptorImpl mailDescriptor) {
        User user = User.get(claimedByUser, false, Collections.emptyMap());
        if (user != null) {
            try {
                final Mailer.UserProperty mailProperty = user.getProperty(Mailer.UserProperty.class);
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
}
