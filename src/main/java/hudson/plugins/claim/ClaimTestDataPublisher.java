package hudson.plugins.claim;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;

public class ClaimTestDataPublisher extends TestDataPublisher {
	
	@DataBoundConstructor
	public ClaimTestDataPublisher() {}
	
	@Override
	public Data getTestData(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener, TestResult testResult) {
		
		Data data = new Data(build);

		for (CaseResult result: testResult.getFailedTests()) {
			CaseResult previous = result.getPreviousResult();
			if (previous != null) {
				ClaimTestAction previousAction = previous.getTestAction(ClaimTestAction.class);
				if (previousAction != null && previousAction.isClaimed() && previousAction.isSticky()) {
					ClaimTestAction action = new ClaimTestAction(data, result.getId());
					previousAction.copyTo(action);
					data.addClaim(result.getId(), action);
				}
			}
		}
		
		return data;
		
	}
	
	public static class Data extends TestResultAction.Data implements Saveable {

		private Map<String,ClaimTestAction> claims = new HashMap<String,ClaimTestAction>();

		private final AbstractBuild<?,?> build;

		public Data(AbstractBuild<?,?> build) {
			this.build = build;
		}

		@Override
		public TestAction getTestAction(TestObject testObject) {
			ClaimTestAction result = claims.get(testObject.getId());
			if (result != null) {
				return result;
			}
			
			if (testObject instanceof CaseResult) {
				CaseResult cr = (CaseResult) testObject;
				if (!cr.isPassed() && !cr.isSkipped()) {
					return new ClaimTestAction(this, testObject.getId());
				}
			}
			
			return null;
		}

		public void save() throws IOException {
			build.save();
		}

		public void addClaim(String testObjectId,
				ClaimTestAction claim) {
			claims.put(testObjectId, claim);
		}
		
	}
	
	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		
		@Override
		public String getDisplayName() {
			return "Allow claiming of failed tests";
		}
	}


}
