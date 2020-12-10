package hudson.plugins.claim;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Api;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.model.TransientActionFactory;

@SuppressWarnings("rawtypes")
@ExportedBean
public class JobClaimsAction extends AbstractAssignedClaimsReport {

    /**
     * Add the {@link JobClaimsAction} to all {@link AbstractProject} instances.
     */
    @Extension(ordinal = -1000)
    public static class TransientJobClaimsActionactoryImpl extends TransientActionFactory<Job> {

        @Override
        public Class<Job> type() {
            return Job.class; 
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull Job job) {
            if (job.getLastCompletedBuild() != null) {
                return Collections.singleton(new JobClaimsAction(job)); 
            }
            
            return Collections.emptySet();
        }
    }
    
    private final Job targetJob;

    public JobClaimsAction(Job target) {
        targetJob = target;
    }
    
    @Override
    public ModelObject getOwner() {
        return targetJob;
    }


    @Override
    protected List<Job> getJobs() {
        return Collections.singletonList(targetJob);
    }


    @Override
    public String getDisplayName() {
        return "Claims";
    }

    @Override
    public String getUrlName() {
        return "claims";
    }
    
    public Api getApi() {
        return new Api(this);
    }
    

    private AbstractTestResultAction<?> getTestResultAction(Job<?, ?> job) {
        Run<?, ?> run = job.getLastCompletedBuild();
        if (run != null) {
            return run.getAction(AbstractTestResultAction.class);
        }
        return null;
    }

    @Exported
    public int getNbUnclaimedTestFailures() {
        AbstractTestResultAction<?> testResultAction = getTestResultAction(targetJob);

        int nbUnclaimedTsetFailures = -1;
        if (testResultAction != null) {
            nbUnclaimedTsetFailures = 0;;
            List<? extends TestResult> failedTests = testResultAction.getFailedTests();
            for (TestResult failedTest : failedTests) {
                ClaimTestAction x = failedTest.getTestAction(ClaimTestAction.class);
                if (x == null || !x.isClaimed()) {
                    nbUnclaimedTsetFailures++;
                }
            }
        }
        return nbUnclaimedTsetFailures;
    }
}
