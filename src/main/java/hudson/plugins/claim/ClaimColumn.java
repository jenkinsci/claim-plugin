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
import hudson.views.ListViewColumn;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

public final class ClaimColumn extends ListViewColumn {

    @DataBoundConstructor
    public ClaimColumn() {
    }

    @Override
    public String getColumnCaption() {
        return Messages.ClaimColumn_ColumnCaption();
    }

    public List<ClaimColumnInformation> getAction(Job<?, ?> job) {
        List<ClaimColumnInformation> result = new ArrayList<>();
        Run<?, ?> run = job.getLastCompletedBuild();
        if (run != null) {
            if (run instanceof hudson.matrix.MatrixBuild) {
                MatrixBuild matrixBuild = (hudson.matrix.MatrixBuild) run;

                for (MatrixRun combination : matrixBuild.getRuns()) {
                    ClaimBuildAction action = ClaimUtils.getBuildAction(combination, true);
                    if (action != null) {
                        Result runResult = combination.getResult();
                        if (runResult != null && runResult.isWorseThan(Result.SUCCESS)) {
                            ClaimColumnInformation holder = new ClaimColumnInformation();
                            holder.setClaim(action);
                            holder.setMatrix(true);
                            holder.setCombinationName(combination.getParent().getCombination().toString() + ": ");
                            result.add(holder);
                        }
                    }
                }
            } else {
                ClaimBuildAction action = ClaimUtils.getBuildAction(run, true);
                if (action != null) {
                    ClaimColumnInformation holder = new ClaimColumnInformation();
                    holder.setClaim(action);
                    result.add(holder);
                }
            }
        }
        return result;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public ListViewColumn newInstance(StaplerRequest req,
                                          @Nonnull JSONObject formData) throws FormException {
            return new ClaimColumn();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ClaimColumn_DisplayName();
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }

    }

}
