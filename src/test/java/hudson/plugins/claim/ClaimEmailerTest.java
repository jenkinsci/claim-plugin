package hudson.plugins.claim;

import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("StringOperationCanBeSimplified")
public class ClaimEmailerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /*
     * Test that no mail is sent if mail sending is not configured
     */
    @Test
    public void testSendEmailNotConfigured() throws Exception {

        final String assigneeId = "assignee";
        final String assignedById = "assignedByMe";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(false);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);

        ClaimEmailer.sendInitialBuildClaimEmailIfConfigured(assignee, assignedBy,
            "Test build", "test reason", "jobs/TestBuild/");

        assertEquals(0, yourInbox.size());
    }

    @Test
    public void testSendEmailOnInitialBuildFailureConfigured() throws Exception {

        final String assigneeId = "assignee";
        final String assignedById = "assignedByMe";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("http://localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the user is existing
        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendInitialBuildClaimEmailIfConfigured(assignee, assignedBy, "Test build",
            "test reason", "jobs/TestBuild/");

        assertEquals(1, yourInbox.size());
        Message mailMessage = yourInbox.get(0);
        Address[] senders = mailMessage.getFrom();
        assertEquals(1, senders.length);
        assertEquals("test <test@test.com>", senders[0].toString());

        String subject = mailMessage.getSubject();
        assertTrue("Mail subject must contain the build name", subject.contains(Messages
            .ClaimEmailer_Build_Initial_Subject("Test build")));

        String content = mailMessage.getContent().toString();
        assertTrue("Mail content should contain the reason", content.contains(Messages
                .ClaimEmailer_Reason("test reason")));
        assertTrue("Mail content should contain the details", content.contains(Messages
                .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/")));
        assertTrue("Mail content should assignment text", content.contains(Messages
            .ClaimEmailer_Build_Initial_Text("Test build", "AssignedBy User")));
    }

    @Test
    public void testSendEmailOnInitialTestFailureConfigured() throws Exception {

        final String assigneeId = "assignee";
        final String assignedById = "assignedByMe";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("http://localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendInitialTestClaimEmailIfConfigured(assignee, assignedBy, "Test Test",
            "test reason", "jobs/TestBuild/testReport/TestTest");

        assertEquals(1, yourInbox.size());
        Message mailMessage = yourInbox.get(0);
        Address[] senders = mailMessage.getFrom();
        assertEquals(1, senders.length);
        assertEquals("test <test@test.com>", senders[0].toString());

        String subject = mailMessage.getSubject();
        assertTrue("Mail subject must contain the build name", subject.contains(Messages
            .ClaimEmailer_Test_Initial_Subject("Test Test")));

        String content = mailMessage.getContent().toString();
        assertTrue("Mail content should contain the reason", content.contains(Messages
            .ClaimEmailer_Reason("test reason")));
        assertTrue("Mail content should contain the details", content.contains(Messages
            .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/testReport/TestTest")));
        assertTrue("Mail content should assignment text",
            content.contains(Messages.ClaimEmailer_Test_Initial_Text("Test Test", "AssignedBy User")));
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

        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendInitialBuildClaimEmailIfConfigured(assignee, assignee, "Test build",
            "test reason", "jobs/TestBuild/");

        assertEquals(0, yourInbox.size());
    }

    /*
     * Test that method does not throw runtime exception if mail is null (can happen when user id contains spaces)
     */
    @Test
    public void shouldNotFailWhenRecipientEmailAddressIsNull() throws Exception {

        final String assigneeId = "sarah connor";
        final String assignedById = "assignedByMe";

        // given
        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(true);

        // ensure the users are existing
        // will generate invalid default mail address with a space
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        // when
        ClaimEmailer.sendInitialBuildClaimEmailIfConfigured(assignee, assignedBy, "Test build",
            "test reason", "jobs/TestBuild/");

        // then
        // no exceptions
    }

    @Test
    public void testSendEmailOnRepeatedBuildFailureConfigured() throws Exception {

        final String assigneeId = "assignee";
        final String assignedById = "assignedByMe";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("http://localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(false);
        config.setSendEmailsForStickyFailures(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);
        ClaimEmailer.sendRepeatedBuildClaimEmailIfConfigured(assignee, "Test build", "jobs/TestBuild/");

        assertEquals(1, yourInbox.size());
        Message mailMessage = yourInbox.get(0);

        Address[] senders = mailMessage.getFrom();
        assertEquals(1, senders.length);
        assertEquals("test <test@test.com>", senders[0].toString());

        String subject = mailMessage.getSubject();
        assertTrue("Mail subject must contain the build name", subject.contains(Messages
            .ClaimEmailer_Build_Repeated_Subject("Test build")));

        String content = mailMessage.getContent().toString();
        assertTrue("Mail content should contain the details", content.contains(Messages
            .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/")));
    }

    /*
     * Test that method does not throw runtime exception if mail is null (can happen when user id contains spaces)
     */
    @Test
    public void shouldNotFailOnRepeatedTestFailureWhenNoTestsAreFailing() throws Exception {

        final String assigneeId = "assignee";
        final String assignedById = "assignedByMe";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("http://localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(false);
        config.setSendEmailsForStickyFailures(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);

        List<CaseResult> tests = new ArrayList<CaseResult>();
        ClaimEmailer.sendRepeatedTestClaimEmailIfConfigured(assignee, "Test Test",
            "jobs/TestBuild/testReport/TestTest", tests);

        assertEquals(0, yourInbox.size());
    }

    @Test
    public void testDontSendEmailIfClaimingForOneself() throws Exception {

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
        ClaimEmailer.sendEmailIfConfigured(assignee, "assignee", "Test build", "test reason", "jobs/TestBuild/");

        assertEquals("we should not send mail if claiming for ourself",0, yourInbox.size());

    }




    @Test
    public void testSendEmailContainsTestUrlWhenClaimingTest() throws Exception {

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

        assertEquals(1,yourInbox.size());
        Address[] senders = yourInbox.get(0).getFrom();
        assertEquals(1,senders.length);
        assertEquals("test <test@test.com>",senders[0].toString());

        //assertEquals(true, false);
        Object content = yourInbox.get(0).getContent();
        assertTrue("Mail content should contain the reason",content.toString().contains(Messages.ClaimEmailer_Reason("test reason")));
        assertTrue("Mail content should contain the details",content.toString().contains(Messages.ClaimEmailer_Details("localhost:8080/jenkins/jobs/TestBuild/")));
        assertTrue("Mail content should assignment text",content.toString().contains(Messages.ClaimEmailer_Text("Test build", "assignedBy")));

    }
        
    @Test
    public void testSendEmailOnRepeatedTestFailureConfigured() throws Exception {

        final String assigneeId = "assignee";
        final String assignedById = "assignedByMe";


        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("http://localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(false);
        config.setSendEmailsForStickyFailures(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox yourInbox = Mailbox.get(new InternetAddress(recipient));
        yourInbox.clear();

        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        UserProperty p = new Mailer.UserProperty("assignee@test.com");
        assignee.addProperty(p);

        CaseResult caseResult1 = mock(CaseResult.class);
        when(caseResult1.getFullDisplayName()).thenReturn("Test 1");
        CaseResult caseResult2 = mock(CaseResult.class);
        when(caseResult2.getFullDisplayName()).thenReturn("Test 2");
        List<CaseResult> tests = new ArrayList<>();
        tests.add(caseResult1);
        tests.add(caseResult2);
        ClaimEmailer.sendRepeatedTestClaimEmailIfConfigured(assignee, "Test Build",
            "jobs/TestBuild/testReport/TestTest", tests);

        assertEquals(1, yourInbox.size());
        Message mailMessage = yourInbox.get(0);

        Address[] senders = mailMessage.getFrom();
        assertEquals(1, senders.length);
        assertEquals("test <test@test.com>", senders[0].toString());

        String subject = mailMessage.getSubject();
        assertTrue("Mail subject must contain the build name and number of failing tests", subject.contains(
            Messages.ClaimEmailer_Test_Repeated_Subject(2, "Test Build")));

        String content = mailMessage.getContent().toString();
        assertTrue("Mail content should contain the test details for test 1", content.contains(Messages
            .ClaimEmailer_Test_Repeated_Details("Test 1")));
        assertTrue("Mail content should contain the test details for test 2", content.contains(Messages
            .ClaimEmailer_Test_Repeated_Details("Test 2")));
        assertTrue("Mail content should contain the details", content.contains(Messages
            .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/testReport/TestTest")));
        assertTrue("Mail content should contain the details", content.contains(Messages
            .ClaimEmailer_Test_Repeated_Text(2, "Test Build")));
    }

    @Test
    public void emailShouldBeSentForStickyBuildClaimWhenReminderConfigured() throws Exception {

        FreeStyleProject job = createFailingJobWithName("test-" + System.currentTimeMillis());
        final String assigneeId = "assignee";
        final String assignedById = "assignedByMe";

        JenkinsLocationConfiguration.get().setAdminAddress("test <test@test.com>");
        JenkinsLocationConfiguration.get().setUrl("localhost:8080/jenkins/");

        ClaimConfig config = ClaimConfig.get();
        config.setSendEmails(false);
        config.setSendEmailsForStickyFailures(true);

        String recipient = "assignee <assignee@test.com>";
        Mailbox recipientInbox = Mailbox.get(new InternetAddress(recipient));
        recipientInbox.clear();

        // ensure the users are existing
        User assignee = User.get(assigneeId, true, Collections.emptyMap());
        assignee.setFullName("Assignee User");
        User assignedBy = User.get(assignedById, true, Collections.emptyMap());
        assignedBy.setFullName("AssignedBy User");

        assignee.addProperty(new Mailer.UserProperty("assignee@test.com"));

        ClaimBuildAction claimAction = job.getLastBuild().getAction(ClaimBuildAction.class);
        claimAction.claim(assignee, "some reason", assignedBy, new Date(),
            true, true, false);

        job.scheduleBuild2(0).get();
        assertEquals(1, recipientInbox.size());
        assertEquals("Assigned build still failing: " + job.getName() + " #2",
            recipientInbox.get(0).getSubject());
    }

    private FreeStyleProject createFailingJobWithName(String jobName) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject(jobName);
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        project.scheduleBuild2(0).get();
        return project;
    }
}
