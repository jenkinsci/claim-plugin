package hudson.plugins.claim;

import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotEquals;

public class UserClaimsReportTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

        java.util.logging.Logger.getLogger("org.htmlunit").setLevel(java.util.logging.Level.SEVERE);
    }

    private FreeStyleProject createFailingJobWithName(String jobName) throws IOException,
            InterruptedException, ExecutionException {
        FreeStyleProject project = j.createFreeStyleProject(jobName);
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        project.scheduleBuild2(0).get();
        return project;
    }

    @Test
    public void userAssignedFailedJobsShouldBeVisibleInUserClaimReport() throws Exception {
        long timestamp = System.currentTimeMillis();

        FreeStyleProject job1 = createFailingJobWithName("job1-" + timestamp);
        FreeStyleProject job2 = createFailingJobWithName("job2-" + timestamp);
        FreeStyleProject job3 = createFailingJobWithName("job3-" + timestamp);
        
        User userA = User.get("userA-" + timestamp, true, Collections.emptyMap());
        User userB = User.get("userB-" + timestamp, true, Collections.emptyMap());
        User userC = User.get("userC-" + timestamp, true, Collections.emptyMap());

        // none claimed
        verifyUserClaims(userA, 0, 0);
        verifyUserClaims(userB, 0, 0);
        verifyUserClaims(userC, 0, 0);

        // job1 claimed by A
        ClaimBuildAction claimAction1 = job1.getLastBuild().getAction(ClaimBuildAction.class);
        claimAction1.applyClaim(userA, "test reason", userB, new Date(), true, true);
        verifyUserClaims(userA, 1, 0);
        verifyUserClaims(userB, 0, 0);
        verifyUserClaims(userC, 0, 0);

        // job2 claimed by C
        ClaimBuildAction claimAction2 = job2.getLastBuild().getAction(ClaimBuildAction.class);
        claimAction2.applyClaim(userC, "test reason", userB, new Date(), true, true);
        verifyUserClaims(userA, 1, 0);
        verifyUserClaims(userB, 0, 0);
        verifyUserClaims(userC, 1, 0);

        // job3 claimed by C
        ClaimBuildAction claimAction3 = job3.getLastBuild().getAction(ClaimBuildAction.class);
        claimAction3.applyClaim(userC, "test reason", userA, new Date(), true, true);
        verifyUserClaims(userA, 1, 0);
        verifyUserClaims(userB, 0, 0);
        verifyUserClaims(userC, 2, 0);
    }

    @Test
    public void userAssignedFailedJobsUnderADifferentIdForSameUserShouldBeVisibleInUserClaimReport() throws Exception {
        long timestamp = System.currentTimeMillis();

        FreeStyleProject job1 = createFailingJobWithName("job1-" + timestamp);

        User userA = User.get("userA-" + timestamp, true, Collections.emptyMap());
        assertNotEquals(userA.getId(), userA.getId().toLowerCase(), "Fix the test setup to ensure this condition");
        User userALowerCasedId = User.get(userA.getId().toLowerCase(), true, Collections.emptyMap());

        // none claimed
        verifyUserClaims(userA, 0, 0);

        // job1 claimed by A
        ClaimBuildAction claimAction1 = job1.getLastBuild().getAction(ClaimBuildAction.class);
        claimAction1.applyClaim(userALowerCasedId, "test reason", userA, new Date(), true, true);

        verifyUserClaims(userA, 1, 0);
    }

    private void verifyUserClaims(User user, int nbProjectClaims, int nbTestFailureClaims) throws Exception {
        try(JenkinsRule.WebClient client = j.createWebClient()) {
            HtmlPage page = client.goTo("user/" + user.getId() + "/claims/");
            j.assertXPathValue(page, "id('claim-nb-project-claims')", String.valueOf(nbProjectClaims));
            j.assertXPathValue(page, "id('claim-nb-testfailure-claims')", String.valueOf(nbTestFailureClaims));
        }
    }
}
