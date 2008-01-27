package hudson.plugins.claim;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * @plugin
 */
public class PluginImpl extends Plugin {
	
	@Override
	public void start() throws Exception {
		BuildStep.PUBLISHERS.addRecorder(ClaimPublisher.DESCRIPTOR);
	}
}
