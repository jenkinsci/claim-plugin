package hudson.plugins.claim;

import hudson.model.UserProperty;
import hudson.model.User;
import hudson.tasks.Mailer;

import java.util.Collections;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import jenkins.model.JenkinsLocationConfiguration;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClaimEmailerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /*
     * Test that no mail is sent if mail sending is not configured
     */
    @Test
    public void testSendEmailNotConfigured() throws Exception {

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(false);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        User assignee = User.get("assignee", true, Collections.emptyMap());
        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendEmailIfConfigured(assignee, "assignedByMe", "Test build", "test reason", "jobs/TestBuild/");

        assertEquals(0, yourInbox.size());
    }

    /*
     * Test that mail is sent to the assignee if mail sending is configured
     */
    @Test
    public void testSendEmailConfigured() throws Exception {

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        User assignee = User.get("assignee", true, Collections.emptyMap());
        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendEmailIfConfigured(assignee, "assignedBy", "Test build", "test reason", "jobs/TestBuild/");

        assertEquals(1, yourInbox.size());
        Address[] senders = yourInbox.get(0).getFrom();
        assertEquals(1, senders.length);
        assertEquals("test <test@test.com>", senders[0].toString());

        Object content = yourInbox.get(0).getContent();
        assertTrue("Mail content should contain the reason", content.toString().contains(Messages
                .ClaimEmailer_Reason("test reason")));
        assertTrue("Mail content should contain the details", content.toString().contains(Messages
                .ClaimEmailer_Details("localhost:8080/jenkins/jobs/TestBuild/")));
        assertTrue("Mail content should assignment text",
                content.toString().contains(Messages.ClaimEmailer_Text("Test build", "assignedBy")));
    }


}
