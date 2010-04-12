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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
		public List<TestAction> getTestAction(TestObject testObject) {
			String id = testObject.getId();
			ClaimTestAction result = claims.get(id);

			// In Hudson 1.347 or so, IDs changed, and a junit/ prefix was added.
			// Attempt to fix this backward-incompatibility
			if (result == null && id.startsWith("junit")) {
				result = claims.get(id.substring(5));
			}
			
			if (result != null) {
				return Collections.<TestAction>singletonList(result);
			}
			
			if (testObject instanceof CaseResult) {
				CaseResult cr = (CaseResult) testObject;
				if (!cr.isPassed() && !cr.isSkipped()) {
					return Collections.<TestAction>singletonList(new ClaimTestAction(this, id));
				}
			}
			
			return Collections.emptyList();
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
