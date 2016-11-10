package hudson.plugins.claim;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.ListView;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ClaimWorkflowTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static final String JOB_NAME = "myjob";
    private WorkflowJob job;
    private ListView view;

    @Before
    public void setUp() throws Exception {

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.FINE);

        job = createFailingJobWithName(JOB_NAME);

        view = new ListView("DefaultView");
        j.jenkins.addView(view);
        j.jenkins.setPrimaryView(view);

    }

    private WorkflowJob createFailingJobWithName(String jobName) throws IOException,
            InterruptedException, ExecutionException {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, jobName);
        job.setDefinition(new CpsFlowDefinition(""
                + "node {\n"
                + "  catchError {\n"
                + "    error('Error')\n"
                + "  }\n"
                + "  step([$class: 'ClaimPublisher'])\n"
                + "}"));
        job.scheduleBuild2(0).get();
        return job;
    }

    @Test
    public void job_is_visible_in_claim_report() throws Exception {
        // Given:
        view.add(job);
        //j.interactiveBreak();
        // When:
        HtmlPage page = j.createWebClient().goTo("claims/");
        // Then:
        DomElement element = page.getElementById("claim.build." + JOB_NAME);
        assertThat(element.isDisplayed(), is(true));
    }

    @Test
    public void job_not_present_in_default_view_is_visible_in_claim_report() throws Exception {
        // When:
        HtmlPage page = j.createWebClient().goTo("claims/");
        // Then:
        DomElement element = page.getElementById("claim.build." + JOB_NAME);
        assertThat(element.isDisplayed(), is(true));
    }

}
