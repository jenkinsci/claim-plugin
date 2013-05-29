package hudson.plugins.claim;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;

public class QuarantineTestDataPublisher extends TestDataPublisher {
	
	@DataBoundConstructor
	public QuarantineTestDataPublisher() {}
	
	@Override
	public Data getTestData(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener, TestResult testResult) {
		
		Data data = new Data(build);


		
		for (SuiteResult suite: testResult.getSuites())
		{
			for (CaseResult result: suite.getCases()) {
				CaseResult previous = result.getPreviousResult();
				if (previous != null) {
					QuarantineTestAction previousAction = previous.getTestAction(QuarantineTestAction.class);

					if (previousAction != null && previousAction.isQuarantined()) {
						QuarantineTestAction action = new QuarantineTestAction(data, result.getId());
						action.quarantine(previousAction);
						data.addQuarantine(result.getId(), action);
					}
				}
			}
		}
		return data;
		
	}
	
	
	public static class Data extends TestResultAction.Data implements Saveable {

		private Map<String,QuarantineTestAction> quarantines = new HashMap<String,QuarantineTestAction>();

		private final AbstractBuild<?,?> build;

		public Data(AbstractBuild<?,?> build) {
			this.build = build;
		}
		
		@Override
		public List<TestAction> getTestAction(TestObject testObject) {
			
			if (build.getParent().getPublishersList().get(QuarantinableJUnitResultArchiver.class) == null)
			{
				// only display if QuarantinableJUnitResultArchiver chosen, to avoid confusion
				System.out.println("not right publisher");
				return Collections.emptyList();
			}
			
			String id = testObject.getId();
			QuarantineTestAction result = quarantines.get(id);
			
			if (result != null) {
				return Collections.<TestAction>singletonList(result);
			}
			
			if (testObject instanceof CaseResult) {
				return Collections.<TestAction>singletonList(new QuarantineTestAction(this, id));
			}
			return Collections.emptyList();
		}
		
		public boolean isLatestResult()
		{
			return build.getParent().getLastBuild() == build;
		}

		public void save() throws IOException {
			build.save();
		}
		
		public void addQuarantine(String testObjectId,
				QuarantineTestAction quarantine) {
				quarantines.put(testObjectId, quarantine);
		}
		
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		
		public String getHelpFile() {
			return "/plugin/claim/help-quarantine.html";
		}
		
		@Override
		public String getDisplayName() {
			return Messages.QuarantineTestDataPublisher_DisplayName();
		}
	}


}
