package hudson.plugins.claim;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AllowBrokenBuildClaimingJobPropertyTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void failedJobWithPropertyShouldHaveClaimBuildAction() throws Exception {
        WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "test-" + System.currentTimeMillis());
        workflowJob.setDefinition(new CpsFlowDefinition(""
                + "node {\n"
                + "  properties([allowBrokenBuildClaiming()])\n"
                + "  error('Error')\n"
                + "}", true));
        workflowJob.scheduleBuild2(0).get();
        
        ClaimBuildAction claimAction = workflowJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNotNull(claimAction);
    }


    @Test
    public void failedJobWithoutPropertyShouldNotHaveClaimBuildAction() throws Exception {
        WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "test-" + System.currentTimeMillis());
        workflowJob.setDefinition(new CpsFlowDefinition(""
                + "node {\n"
                + "  error('Error')\n"
                + "}", true));
        workflowJob.scheduleBuild2(0).get();
        
        ClaimBuildAction claimAction = workflowJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNull(claimAction);
    }

    @Test
    public void failedJobWithPropertyAndStepShouldHaveClaimBuildAction() throws Exception {
        WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "test-" + System.currentTimeMillis());
        workflowJob.setDefinition(new CpsFlowDefinition(""
                + "node {\n"
                + "  properties([allowBrokenBuildClaiming()])\n"
                + "  currentBuild.result = 'ERROR'\n"
                + "  step([$class: 'ClaimPublisher'])\n"
                + "  error('Error')\n"
                + "}", true));
        workflowJob.scheduleBuild2(0).get();
        
        ClaimBuildAction claimAction = workflowJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNotNull(claimAction);
    }
}
