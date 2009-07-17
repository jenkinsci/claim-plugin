package hudson.plugins.claim;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.tasks.BuildStep;

public class PluginImpl extends Plugin {
	@Override
	public void start() throws Exception {
		Hudson.getInstance().getActions().add(new ClaimedBuildsReport(Hudson.getInstance()));
	}
}
