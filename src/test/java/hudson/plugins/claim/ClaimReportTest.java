package hudson.plugins.claim;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class ClaimReportTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
        java.util.logging.Logger.getLogger("org.htmlunit").setLevel(java.util.logging.Level.SEVERE);
    }

    private FreeStyleProject createFailingJobWithName(String jobName) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject(jobName);
        project.getBuildersList().add(new FailureBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        project.scheduleBuild2(0).get();
        return project;
    }

    @Test
    void claimedFailedJobShouldBeVisibleInClaimReport() throws Exception {
        String jobName = "failing-" + System.currentTimeMillis();
        FreeStyleProject claimedJob = createFailingJobWithName(jobName);
        ClaimBuildAction claimAction = claimedJob.getLastBuild().getAction(ClaimBuildAction.class);

        User user1 = User.get("test-user1", true, Collections.emptyMap());
        User user2 = User.get("test-user2", true, Collections.emptyMap());
        claimAction.applyClaim(user1, "test reason", user2, new Date(), true, true);

        try(JenkinsRule.WebClient client = j.createWebClient()) {
            HtmlPage page = client.goTo("claims/");
            DomElement element = page.getElementById("claim.build." + jobName);
            assertNotNull(element);
            assertTrue(element.isDisplayed());
        }
    }

    @Test
    void unclaimedFailedJobShouldNotBeVisibleInClaimReport() throws Exception {
        String jobName = "failing-" + System.currentTimeMillis();
        FreeStyleProject unclaimedJob = createFailingJobWithName(jobName);

        ClaimBuildAction claimAction = unclaimedJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNotNull(claimAction);
        assertFalse(claimAction.isClaimed());

        try(JenkinsRule.WebClient client = j.createWebClient()) {
            HtmlPage page = client.goTo("claims/");
            DomElement element = page.getElementById("claim.build." + jobName);
            assertNull(element);
        }
    }
}
