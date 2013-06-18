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
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestResultAction;
import hudson.util.DescribableList;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import java.io.IOException;

public class QuarantineCoreTest extends HudsonTestCase {
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

	
	
	
	public void testAllTestsHaveQuarantineAction() throws Exception {
    	TestResult tr = getResultsFromJUnitResult("junit-1-failure.xml");
    	
    	for (SuiteResult suite: tr.getSuites())
    	{
			for (CaseResult result: suite.getCases())
			{
				assertNotNull(result.getTestAction(QuarantineTestAction.class));
			}
    	}		
    }

	public void testNoTestsHaveQuarantineActionForStandardPublisher() throws Exception {
		project.getPublishersList().remove(QuarantinableJUnitResultArchiver.class);
		
	    DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> publishers =
	        new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(project);
	    publishers.add(new QuarantineTestDataPublisher());
	    project.getPublishersList().add(new JUnitResultArchiver("*.xml",false, publishers));

    	TestResult tr = getResultsFromJUnitResult("junit-1-failure.xml");
    	
    	for (SuiteResult suite: tr.getSuites())
    	{
			for (CaseResult result: suite.getCases())
			{
				assertNull(result.getTestAction(QuarantineTestAction.class));
			}
    	}		
    }
	
	
	public void testQuarantineSetAndRelease() throws Exception {
    	TestResult tr = getResultsFromJUnitResult("junit-1-failure.xml");
    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1", "reason");
        assertTrue(action.isQuarantined());
        action.release();
        assertFalse(action.isQuarantined());
	}
	
    public void testQuarantineIsStickyOnFailingTest() throws Exception {
    	TestResult tr = getResultsFromJUnitResult("junit-1-failure.xml");

    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1", "reason");
        assertTrue(action.isQuarantined());
    	
    	tr = getResultsFromJUnitResult("junit-1-failure.xml");
    	QuarantineTestAction action2 = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);

    	assertTrue(tr.getOwner().getNumber() == 2);
    	assertTrue(action2.isQuarantined());
        assertEquals(action.quarantinedByName(), action2.quarantinedByName());

    }
    
    public void testQuarantineIsStickyOnPassingTest() throws Exception {
    	TestResult tr = getResultsFromJUnitResult("junit-1-failure.xml");

    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestA").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1", "reason");
        assertTrue(action.isQuarantined());
    	
    	tr = getResultsFromJUnitResult("junit-1-failure.xml");
    	QuarantineTestAction action2 = tr.getSuite("SuiteA").getCase("TestA").getTestAction(QuarantineTestAction.class);

    	assertTrue(tr.getOwner().getNumber() == 2);
    	assertTrue(action2.isQuarantined());
        assertEquals(action.quarantinedByName(), action2.quarantinedByName());

    }
    
    public void testResultIsOnlyMarkedAsLatestIfLatest() throws Exception {
    	FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
    	TestResult tr1 = build.getAction(TestResultAction.class).getResult();
    	QuarantineTestAction action1 = tr1.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	
    	assertTrue(action1.isLatestResult());
    	
    	build = runBuildWithJUnitResult("junit-1-failure.xml");    	
    	TestResult tr2 = build.getAction(TestResultAction.class).getResult();
    	QuarantineTestAction action2 = tr2.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);

    	assertFalse(action1.isLatestResult());
    	assertTrue(action2.isLatestResult());
    }
    
    public void testQuarantiningMakesFinalResultPass() throws Exception  {
    	FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");    	
    	assertTrue(build.getResult() != Result.SUCCESS);
    	
    	TestResult tr = build.getAction(TestResultAction.class).getResult();
    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1","reason");
    	
    	build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() == Result.SUCCESS);
    }

    public void testQuarantiningMakesFinalResultFailIfAnotherTestFails() throws Exception  {
    	FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() != Result.SUCCESS);
    	
    	TestResult tr = build.getAction(TestResultAction.class).getResult();
    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1","reason");
    	
    	build = runBuildWithJUnitResult("junit-2-failures.xml");
    	assertTrue(build.getResult() != Result.SUCCESS);
    }

    public void testQuarantiningMakesFinalResultFailIfQuarantineReleased() throws Exception  {
    	FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() != Result.SUCCESS);
    	
    	TestResult tr = build.getAction(TestResultAction.class).getResult();
    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1","reason");
    	
    	build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() == Result.SUCCESS);
    	tr = build.getAction(TestResultAction.class).getResult();
    	action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.release();
    	
    	build = runBuildWithJUnitResult("junit-1-failure.xml");
    	System.out.println("result is " + build.getResult());
    	assertTrue(build.getResult() != Result.SUCCESS);
   	
    }
    
    public void testQuarantinedTestsAreInReport() throws Exception  
    {
    	TestResult tr = getResultsFromJUnitResult("junit-1-failure.xml");
    	
    	tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class).quarantine("user1","reason");
    	tr.getSuite("SuiteB").getCase("TestA").getTestAction(QuarantineTestAction.class).quarantine("user1","reason");
    	
    	QuarantinedTestsReport report = new QuarantinedTestsReport();
    	
    	assertEquals(2,report.getQuarantinedTests().size());
    	assertTrue(report.getQuarantinedTests().contains(tr.getSuite("SuiteA").getCase("TestB")));
    	assertTrue(report.getQuarantinedTests().contains(tr.getSuite("SuiteB").getCase("TestA")));
    }

    public void testQuarantineReportGetNumberOfSuccessivePasses() throws Exception
    {
    	TestResult tr = getResultsFromJUnitResult("junit-no-failure.xml");
    	tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class).quarantine("user1","reason");

    	QuarantinedTestsReport report = new QuarantinedTestsReport();
    	assertEquals(1,report.getNumberOfSuccessivePasses(report.getQuarantinedTests().get(0)));
    	
    	runBuildWithJUnitResult("junit-no-failure.xml");
    	report = new QuarantinedTestsReport();
    	assertEquals(2,report.getNumberOfSuccessivePasses(report.getQuarantinedTests().get(0)));
   	
    	runBuildWithJUnitResult("junit-1-failure.xml");
    	report = new QuarantinedTestsReport();
    	assertEquals(0,report.getNumberOfSuccessivePasses(report.getQuarantinedTests().get(0)));
   	
    	runBuildWithJUnitResult("junit-no-failure.xml");
    	report = new QuarantinedTestsReport();
    	assertEquals(1,report.getNumberOfSuccessivePasses(report.getQuarantinedTests().get(0)));
    }

    
}
