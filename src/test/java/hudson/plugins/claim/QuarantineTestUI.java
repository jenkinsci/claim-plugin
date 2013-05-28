package hudson.plugins.claim;

import hudson.model.FreeStyleBuild;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;

import java.io.IOException;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;

public class QuarantineTestUI extends QuarantineTest {

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
