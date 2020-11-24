package hudson.plugins.claim;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;
import hudson.model.User;

public class ClaimReportTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);
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
    public void claimedFailedJobShouldBeVisibleInClaimReport() throws Exception {
    	String jobName = "failing-" + System.currentTimeMillis();
        FreeStyleProject claimedJob = createFailingJobWithName(jobName);
        ClaimBuildAction claimAction = claimedJob.getLastBuild().getAction(ClaimBuildAction.class);
        
        User user1 = User.get("test-user1", true, Collections.emptyMap());
        User user2 = User.get("test-user2", true, Collections.emptyMap());
        claimAction.applyClaim(user1.getId(), "test reason", user2.getId(), new Date(), true, true);

        HtmlPage page = j.createWebClient().goTo("claims/");
        DomElement element = page.getElementById("claim.build." + jobName);
        assertNotNull(element);
        assertTrue(element.isDisplayed());
    }

    @Test
    public void unclaimedFailedJobShouldNotBeVisibleInClaimReport() throws Exception {
    	String jobName = "failing-" + System.currentTimeMillis();
        FreeStyleProject unclaimedJob = createFailingJobWithName(jobName);
        
        ClaimBuildAction claimAction = unclaimedJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNotNull(claimAction);
        assertFalse(claimAction.isClaimed());
        
        HtmlPage page = j.createWebClient().goTo("claims/");
        DomElement element = page.getElementById("claim.build." + jobName);
        assertNull(element);
    }
}
