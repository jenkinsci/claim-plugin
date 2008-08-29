package hudson.plugins.claim;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.tasks.BuildStep;

/**
 * @Plugin
 */
public class PluginImpl extends Plugin {
	
	@Override
	public void start() throws Exception {
		BuildStep.PUBLISHERS.addRecorder(ClaimPublisher.DESCRIPTOR);
		Hudson.getInstance().getActions().add(new ClaimedBuildsReport(Hudson.getInstance()));
	}
}
