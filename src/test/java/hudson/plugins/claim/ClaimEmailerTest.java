package hudson.plugins.claim;

import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.mock_javamail.Mailbox;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WithJenkins
class ClaimEmailerTest {

    /*
     * Test that no mail is sent if mail sending is not configured
     */
    @Test
    void testSendEmailNotConfigured(JenkinsRule j) throws Exception {

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
    void testSendEmailOnInitialBuildFailureConfigured(JenkinsRule j) throws Exception {

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
        assertTrue(subject.contains(Messages
            .ClaimEmailer_Build_Initial_Subject("Test build")), "Mail subject must contain the build name");

        String content = mailMessage.getContent().toString();
        assertTrue(content.contains(Messages
                .ClaimEmailer_Reason("test reason")), "Mail content should contain the reason");
        assertTrue(content.contains(Messages
                .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/")), "Mail content should contain the details");
        assertTrue(content.contains(Messages
            .ClaimEmailer_Build_Initial_Text("Test build", "AssignedBy User")), "Mail content should assignment text");
    }

    @Test
    void testSendEmailOnInitialTestFailureConfigured(JenkinsRule j) throws Exception {

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
        assertTrue(subject.contains(Messages
            .ClaimEmailer_Test_Initial_Subject("Test Test")), "Mail subject must contain the build name");

        String content = mailMessage.getContent().toString();
        assertTrue(content.contains(Messages
            .ClaimEmailer_Reason("test reason")), "Mail content should contain the reason");
        assertTrue(content.contains(Messages
            .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/testReport/TestTest")), "Mail content should contain the details");
        assertTrue(content.contains(Messages.ClaimEmailer_Test_Initial_Text("Test Test", "AssignedBy User")),
            "Mail content should assignment text");
    }

    /*
     * Test that mail is not sent when self claiming
     */
    @Test
    void shouldNotSendEmailWhenSelfClaiming(JenkinsRule j) throws Exception {

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
    void shouldNotFailWhenRecipientEmailAddressIsNull(JenkinsRule j) throws Exception {

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
    void testSendEmailOnRepeatedBuildFailureConfigured(JenkinsRule j) throws Exception {

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
        assertTrue(subject.contains(Messages
            .ClaimEmailer_Build_Repeated_Subject("Test build")), "Mail subject must contain the build name");

        String content = mailMessage.getContent().toString();
        assertTrue(content.contains(Messages
            .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/")), "Mail content should contain the details");
    }

    /*
     * Test that method does not throw runtime exception if mail is null (can happen when user id contains spaces)
     */
    @Test
    void shouldNotFailOnRepeatedTestFailureWhenNoTestsAreFailing(JenkinsRule j) throws Exception {

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

        List<CaseResult> tests = new ArrayList<>();
        ClaimEmailer.sendRepeatedTestClaimEmailIfConfigured(assignee, "Test Test",
            "jobs/TestBuild/testReport/TestTest", tests);

        assertEquals(0, yourInbox.size());
    }

    @Test
    void testSendEmailOnRepeatedTestFailureConfigured(JenkinsRule j) throws Exception {

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
        assertTrue(subject.contains(
            Messages.ClaimEmailer_Test_Repeated_Subject(2, "Test Build")), "Mail subject must contain the build name and number of failing tests");

        String content = mailMessage.getContent().toString();
        assertTrue(content.contains(Messages
            .ClaimEmailer_Test_Repeated_Details("Test 1")), "Mail content should contain the test details for test 1");
        assertTrue(content.contains(Messages
            .ClaimEmailer_Test_Repeated_Details("Test 2")), "Mail content should contain the test details for test 2");
        assertTrue(content.contains(Messages
            .ClaimEmailer_Details("http://localhost:8080/jenkins/jobs/TestBuild/testReport/TestTest")), "Mail content should contain the details");
        assertTrue(content.contains(Messages
            .ClaimEmailer_Test_Repeated_Text(2, "Test Build")), "Mail content should contain the details");
    }

    @Test
    void emailShouldBeSentForStickyBuildClaimWhenReminderConfigured(JenkinsRule j) throws Exception {

        FreeStyleProject job = createFailingJobWithName(j, "test-" + System.currentTimeMillis());
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

    private static FreeStyleProject createFailingJobWithName(JenkinsRule j, String jobName) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject(jobName);
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        project.scheduleBuild2(0).get();
        return project;
    }
}
