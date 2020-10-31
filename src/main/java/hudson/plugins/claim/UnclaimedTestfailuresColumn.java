package hudson.plugins.claim;

import hudson.views.ListViewColumnDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import hudson.views.ListViewColumn;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

public final class UnclaimedTestfailuresColumn extends ListViewColumn {

    @DataBoundConstructor
    public UnclaimedTestfailuresColumn() {
    }

    @Override
    public String getColumnCaption() {
        return Messages.UnclaimedTestfailuresColumn_ColumnCaption();
    }

    public UnclaimedTestfailuresColumnInformation getUnclaimedTestsInfo(Job<?, ?> job) {
    	
        AbstractTestResultAction<?> testResultAction = getTestResultAction(job);
        
        UnclaimedTestfailuresColumnInformation info = null;
        if (testResultAction != null) {
        	int nbClaimedFailures = 0;
			List<? extends TestResult> failedTests = testResultAction.getFailedTests();
			for (TestResult failedTest : failedTests) {
				ClaimTestAction x = failedTest.getTestAction(ClaimTestAction.class);
				if (x != null && x.isClaimed()) {
					nbClaimedFailures++;
				}
			}

    		info = new UnclaimedTestfailuresColumnInformation(testResultAction, nbClaimedFailures);
        }
        return info;
    }

	private AbstractTestResultAction<?> getTestResultAction(Job<?, ?> job) {
		Run<?, ?> run = job.getLastCompletedBuild();
		if (run != null) {
			return run.getAction(AbstractTestResultAction.class);
		}
		return null;
	}

	@Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public ListViewColumn newInstance(StaplerRequest req,
                                          @Nonnull JSONObject formData) throws FormException {
            return new UnclaimedTestfailuresColumn();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.UnclaimedTestfailuresColumn_DisplayName();
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }

    }

}
