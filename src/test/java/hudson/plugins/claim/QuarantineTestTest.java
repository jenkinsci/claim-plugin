/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Tom Huybrechts
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.claim;

import java.io.IOException;
import org.xml.sax.SAXException;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import hudson.model.Project;
import hudson.model.FreeStyleBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.BuildListener;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.DescribableList;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;


public class QuarantineTestTest extends HudsonTestCase {
	private String claimText = "claimReason";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.SEVERE);
        hudson.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
        hudson.setSecurityRealm(createDummySecurityRealm());
    }

    public void testAllTestsHaveQuarantineAction() throws Exception {
    	FreeStyleBuild build = configureTestBuild("junit-1-failure.xml");
    	TestResult tr = build.getAction(TestResultAction.class).getResult();
    	
    	for (SuiteResult suite: tr.getSuites())
    	{
			for (CaseResult result: suite.getCases())
			{
				assertNotNull(result.getTestAction(QuarantineTestAction.class));
			}
    	}		
    }
    
    private FreeStyleBuild configureTestBuild(final String xmlFileName) throws Exception {
    	FreeStyleProject p = createFreeStyleProject("x");
        p.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("junit.xml").copyFrom(
                    getClass().getResource(xmlFileName));
                return true;
            }
        });
        DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> publishers =
            new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(p);
        publishers.add(new ClaimTestDataPublisher());
        
        p.getPublishersList().add(new JUnitResultArchiver("*.xml",false, publishers));
    	return p.scheduleBuild2(0).get();
    }
}
