package hudson.plugins.claim;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.util.DescribableList;

import java.io.IOException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;


public class QuarantineTest extends HudsonTestCase {

	private String projectName = "x";
	protected String quarantineText = "quarantineReason";
	protected FreeStyleProject project;

	@Override
	protected void setUp() throws Exception {
	    super.setUp();
	    java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);
	    
		project = createFreeStyleProject(projectName);
	    DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> publishers =
	        new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(project);
	    publishers.add(new QuarantineTestDataPublisher());
	    project.getPublishersList().add(new QuarantinableJUnitResultArchiver("*.xml",false, publishers));
		
	    hudson.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
	    hudson.setSecurityRealm(createDummySecurityRealm());
	}

	protected FreeStyleBuild runBuildWithJUnitResult(final String xmlFileName) throws Exception {
		FreeStyleBuild build;
	    project.getBuildersList().add(new TestBuilder() {
	        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
	            build.getWorkspace().child("junit.xml").copyFrom(
	                getClass().getResource(xmlFileName));
	            return true;
	        }
	    });
	    build = project.scheduleBuild2(0).get();
	    project.getBuildersList().clear();
	    return build;
	}

	protected TestResult getResultsFromJUnitResult(final String xmlFileName) throws Exception {
		return runBuildWithJUnitResult(xmlFileName).getAction(TestResultAction.class).getResult();
	}

	/**
	 * only needed to make JUnit happy - is there a better way?
	 */
	public void testDummy() {
		
	}
}
