package hudson.plugins.claim;

import hudson.model.FreeStyleBuild;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.DescribableList;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;

public class QuarantineTestUI extends HudsonTestCase {
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

	
	
	public void testTextSummaryForUnquarantinedTestAuthenticated()
			throws Exception {
				FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
				TestResult tr = build.getAction(TestResultAction.class).getResult();
				HtmlPage page = whenNavigatingToTestCase(tr.getSuite("SuiteA").getCase("TestA"),true);
				
				assertTrue(pageShowsText(page,"This test was not quarantined. Quarantine it."));
			}

	public void testTextSummaryForUnquarantinedTestNotAuthenticated()
			throws Exception {
				FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
				TestResult tr = build.getAction(TestResultAction.class).getResult();
				HtmlPage page = whenNavigatingToTestCase(tr.getSuite("SuiteA").getCase("TestA"),false);
				
				assertTrue(pageShowsText(page,"This test was not quarantined."));
				assertFalse(pageShowsText(page,"Quarantine it."));
			}

	public void testWhenQuarantiningTestSaysQuarantinedBy() throws Exception {
		FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
		TestResult tr = build.getAction(TestResultAction.class).getResult();
		HtmlPage page = whenNavigatingToTestCase(tr.getSuite("SuiteA").getCase("TestA"),true);
		whenQuarantiningTestOnPage(page);
		
		page = whenNavigatingToTestCase(tr.getSuite("SuiteA").getCase("TestA"),false);
		assertTrue(pageShowsText(page,"This test was quarantined by user1"));
	}

	private HtmlPage whenNavigatingToTestCase(CaseResult testCase, boolean authenticate)
			throws Exception, IOException, SAXException {
				WebClient wc = new WebClient();
				if (authenticate)
				{
					wc.login("user1", "user1");
				}
			    HtmlPage page = wc.goTo(testCase.getOwner().getUrl() + "testReport/" + testCase.getUrl());
				return page;
			}

	private void whenQuarantiningTestOnPage(HtmlPage page) throws Exception {
		((HtmlAnchor) page.getElementById("quarantine")).click();
	    HtmlForm form = page.getFormByName("quarantine");
	    HtmlTextArea textArea = (HtmlTextArea) last(form.selectNodes(".//textarea"));
	    textArea.setText(quarantineText);
	    
	    form.submit((HtmlButton) last(form.selectNodes(".//button")));
	}

	private boolean pageShowsText(HtmlPage page, String text) {
		return page.asText().indexOf(text) != -1;
	}

}
