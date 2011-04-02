package hudson.plugins.claim;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.views.ListViewColumn;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class ClaimColumn extends ListViewColumn {
	
	@DataBoundConstructor
	public ClaimColumn() {
	}

	@Override
	public String getColumnCaption() {
		return "Claim";
	}

	@Override
	public boolean shownByDefault() {
		return false;
	}
	
	public List<ClaimBuildAction> getAction(Job<?,?> job) {
                List<ClaimBuildAction> result = new ArrayList<ClaimBuildAction>();
		Run<?,?> run = job.getLastCompletedBuild();
		if (run != null) {
                    if (run instanceof hudson.matrix.MatrixBuild) {
                        MatrixBuild matrixBuild = (hudson.matrix.MatrixBuild) run;
                        
                        for (MatrixRun combination : matrixBuild.getRuns()) {
                            ClaimBuildAction action = combination.getAction(ClaimBuildAction.class);
                            if (combination.getResult().isWorseThan(Result.SUCCESS) && action != null && action.isClaimed()) {
                                result.add(action);
                            }
                        }
                    } else {
                        ClaimBuildAction action = run.getAction(ClaimBuildAction.class);
                        if (action != null && action.isClaimed()) {
                            result.add(action);
                        }
                    }
		}
                return result;
	}

	public Descriptor<ListViewColumn> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(getClass());
	}
	
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	
	@Extension
	public static class DescriptorImpl extends Descriptor<ListViewColumn> {
        @Override
        public ListViewColumn newInstance(StaplerRequest req,
                                          JSONObject formData) throws FormException {
            return new ClaimColumn();
        }

		@Override
		public String getDisplayName() {
			return "Claim";
		}
		
	}

}
