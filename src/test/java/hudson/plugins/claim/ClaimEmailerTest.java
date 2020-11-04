package hudson.plugins.claim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;

import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.tasks.Mailer;
import jenkins.model.JenkinsLocationConfiguration;

public class ClaimEmailerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /*
     * Test that no mail is sent if mail sending is not configured
     */
    @Test
    public void testSendEmailNotConfigured() throws Exception {

        final String assigneeId = "assignee";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(false);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the user is existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendEmailIfConfigured(assigneeId, "assignedByMe", "Test build", "test reason", "jobs/TestBuild/");

        assertEquals(0, yourInbox.size());
    }

    /*
     * Test that mail is sent to the assignee if mail sending is configured
     */
    @Test
    public void testSendEmailConfigured() throws Exception {

        final String assigneeId = "assignee";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("http://localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the user is existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendEmailIfConfigured(assigneeId, "assignedBy", "Test build", "test reason", "jobs/TestBuild/");

        assertEquals(1, yourInbox.size());
        Address[] senders = yourInbox.get(0).getFrom();
        assertEquals(1, senders.length);
        assertEquals("test <test@test.com>", senders[0].toString());

        Object content = yourInbox.get(0).getContent();
        assertTrue("Mail content should contain the reason", content.toString().contains(Messages
                .ClaimEmailer_Reason("test reason")));
        assertTrue("Mail content should contain the details", content.toString().contains(Messages
                .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/")));
        assertTrue("Mail content should assignment text",
                content.toString().contains(Messages.ClaimEmailer_Text("Test build", "assignedBy")));
    }

    /*
     * Test that mail is not sent when self claiming
     */
    @Test
    public void shouldNotSendEmailWhenSelfClaiming() throws Exception {

        final String assigneeId = "assignee";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the user is existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendEmailIfConfigured(assigneeId, assigneeId, "Test build", "test reason", "jobs/TestBuild/");

        assertEquals(0, yourInbox.size());
    }

    /*
     * Test that method does not throw runtime exception if mail is null (can happen when user id contains spaces)
     */
    @Test
    public void shouldNotFailWhenRecipientEmailAddressIsNull() throws Exception {

        final String assigneeId = "sarah connor";

        // given
        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);

        // ensure the user is existing, will generate invalid default mail address with a space
        User.get(assigneeId, true, Collections.emptyMap());

        // when
        ClaimEmailer.sendEmailIfConfigured(assigneeId, "assignedBy", "Test build", "test reason", "jobs/TestBuild/");

        // then
        // no exceptions
    }

    /*
     */
    @Test
    public void emailShouldBeSentForStickyClaimWhenReminderConfigured() throws Exception {

    	FreeStyleProject job = createFailingJobWithName("test-" + System.currentTimeMillis()); 
        final String assigneeId = "assignee";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);
        config.setSendEmailsForStickyFailures(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox recipientInbox = Mailbox.get(new InternetAddress(recipient));
        recipientInbox.clear();

        // ensure the user is existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.addProperty(new Mailer.UserProperty("assignee@test.com"));
        
        ClaimBuildAction claimAction = job.getLastBuild().getAction(ClaimBuildAction.class);
        claimAction.claim(assignee.getId(), "some reason", "assignedByUser", new Date(), true, true, false);

        job.scheduleBuild2(0).get();
        assertEquals(1, recipientInbox.size());
        assertEquals("Assigned job still failing: " + job.getName() + " #2", recipientInbox.get(0).getSubject());
    }

    private FreeStyleProject createFailingJobWithName(String jobName) throws Exception {
		FreeStyleProject project = j.createFreeStyleProject(jobName);
		project.getBuildersList().add(new FailureBuilder());
		project.getPublishersList().add(new ClaimPublisher());
		project.scheduleBuild2(0).get();
		return project;
	}
}
