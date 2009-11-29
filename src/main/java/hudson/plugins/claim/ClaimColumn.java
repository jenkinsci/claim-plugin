package hudson.plugins.claim;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import hudson.views.ListViewColumn;

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
	
	public ClaimBuildAction getAction(Job<?,?> job) {
		Run<?,?> run = job.getLastCompletedBuild();
		if (run == null) {
			return null;
		}
		return run.getAction(ClaimBuildAction.class);
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
