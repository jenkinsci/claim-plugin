package hudson.plugins.claim;

import java.io.IOException;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ClaimPublisher extends Publisher {

	public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

	public ClaimPublisher() {
	}
	
	@Override
	public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}
	
	private static class DescriptorImpl extends Descriptor<Publisher> {

		protected DescriptorImpl() {
			super(ClaimPublisher.class);
		}

		@Override
		public String getHelpFile() {
			return "/plugin/claim/help.html";
		}

		@Override
		public String getDisplayName() {
			return "Allow broken build claiming";
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			return new ClaimPublisher();
		}
		
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		
		if (build.getResult().isWorseThan(Result.SUCCESS)) {
			ClaimBuildAction action = new ClaimBuildAction(build);
			build.addAction(action);

			// check if previous build was claimed
			AbstractBuild<?,?> previousBuild = build.getPreviousBuild();
			if (previousBuild != null) {
				ClaimBuildAction c = previousBuild.getAction(ClaimBuildAction.class);
				if (c != null && c.isClaimed()) {
					action.claim(c.getClaimedBy());
				}
			}
		}
		
		return true;
	}
	
}
