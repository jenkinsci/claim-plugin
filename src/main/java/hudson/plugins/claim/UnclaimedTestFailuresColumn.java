package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.List;

public final class UnclaimedTestFailuresColumn extends ListViewColumn {

    @DataBoundConstructor
    public UnclaimedTestFailuresColumn() {
    }

    @Override
    public String getColumnCaption() {
        return Messages.UnclaimedTestFailuresColumn_ColumnCaption();
    }

    public UnclaimedTestFailuresColumnInformation getUnclaimedTestsInfo(Job<?, ?> job) {
        AbstractTestResultAction<?> testResultAction = getTestResultAction(job);

        UnclaimedTestFailuresColumnInformation info = null;
        if (testResultAction != null) {
            int nbClaimedFailures = 0;
            List<? extends TestResult> failedTests = testResultAction.getFailedTests();
            for (TestResult failedTest : failedTests) {
                ClaimTestAction x = failedTest.getTestAction(ClaimTestAction.class);
                if (x != null && x.isClaimed()) {
                    nbClaimedFailures++;
                }
            }

            info = new UnclaimedTestFailuresColumnInformation(testResultAction, nbClaimedFailures);
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
        public ListViewColumn newInstance(StaplerRequest2 req, @NonNull JSONObject formData) {
            return new UnclaimedTestFailuresColumn();
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.UnclaimedTestFailuresColumn_DisplayName();
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }

    }
}
