package hudson.plugins.claim;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
class AllowBrokenBuildClaimingJobPropertyTest {

    @Test
    void failedJobWithPropertyShouldHaveClaimBuildAction(JenkinsRule j) throws Exception {
        WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "test-" + System.currentTimeMillis());
        workflowJob.setDefinition(new CpsFlowDefinition("""
                node {
                  properties([allowBrokenBuildClaiming()])
                  error('Error')
                }""", true));
        workflowJob.scheduleBuild2(0).get();

        ClaimBuildAction claimAction = workflowJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNotNull(claimAction);
    }


    @Test
    void failedJobWithoutPropertyShouldNotHaveClaimBuildAction(JenkinsRule j) throws Exception {
        WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "test-" + System.currentTimeMillis());
        workflowJob.setDefinition(new CpsFlowDefinition("""
                node {
                  error('Error')
                }""", true));
        workflowJob.scheduleBuild2(0).get();

        ClaimBuildAction claimAction = workflowJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNull(claimAction);
    }

    @Test
    void failedJobWithPropertyAndStepShouldHaveClaimBuildAction(JenkinsRule j) throws Exception {
        WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "test-" + System.currentTimeMillis());
        workflowJob.setDefinition(new CpsFlowDefinition("""
                node {
                  properties([allowBrokenBuildClaiming()])
                  currentBuild.result = 'ERROR'
                  step([$class: 'ClaimPublisher'])
                  error('Error')
                }""", true));
        workflowJob.scheduleBuild2(0).get();

        ClaimBuildAction claimAction = workflowJob.getLastBuild().getAction(ClaimBuildAction.class);
        assertNotNull(claimAction);
    }
}
