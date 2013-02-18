package hudson.plugins.claim;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import hudson.model.FreeStyleProject;
import hudson.model.ListView;

import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class ClaimReportTest extends HudsonTestCase {


	private static final String JOB_NAME = "job";
	private FreeStyleProject job;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);

		job = createFailingJobWithName(JOB_NAME);

	}

	private FreeStyleProject createFailingJobWithName(String jobName) throws IOException,
	InterruptedException, ExecutionException {
		FreeStyleProject project = createFreeStyleProject(jobName);
		project.getBuildersList().add(new FailureBuilder());
		project.getPublishersList().add(new ClaimPublisher());
		project.scheduleBuild2(0).get();
		return project;
	}

	public void testThatJobNotPresentInDefaultViewIsNotVisibleInClaimReport() throws Exception {
		ListView view = new ListView("DefaultView");
		hudson.addView(view);
		hudson.setPrimaryView(view);

		WebClient wc = new WebClient();
		
		HtmlPage page = wc.goTo("claims/");
		HtmlElement element = page.getElementById("no-failing-builds");
		assertTrue(element.isDisplayed());
	}
	
	public void testJobPresentInDefaultViewIsVisibleInClaimReport() throws Exception {
		ListView view = new ListView("DefaultView");
		view.add(job);
		hudson.addView(view);
		hudson.setPrimaryView(view);
		
		WebClient wc = new WebClient();
		
		HtmlPage page = wc.goTo("claims/");
		HtmlElement element = page.getElementById("claim.build." + JOB_NAME);
		assertTrue(element.isDisplayed());
	}
	
	
}
