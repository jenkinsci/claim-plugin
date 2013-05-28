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
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;



public class QuarantineTestCore extends QuarantineTest {
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
    
    public void testQuarantiningMakesFinalResultPass() throws Exception  {
    	FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() == Result.UNSTABLE);
    	
    	TestResult tr = build.getAction(TestResultAction.class).getResult();
    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1","reason");
    	
    	build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() == Result.SUCCESS);
    }

    public void testQuarantiningMakesFinalResultFailIfAnotherTestFails() throws Exception  {
    	FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() == Result.UNSTABLE);
    	
    	TestResult tr = build.getAction(TestResultAction.class).getResult();
    	QuarantineTestAction action = tr.getSuite("SuiteA").getCase("TestB").getTestAction(QuarantineTestAction.class);
    	action.quarantine("user1","reason");
    	
    	build = runBuildWithJUnitResult("junit-2-failures.xml");
    	assertTrue(build.getResult() == Result.UNSTABLE);
    }

    public void testQuarantiningMakesFinalResultFailIfQuarantineReleased() throws Exception  {
    	FreeStyleBuild build = runBuildWithJUnitResult("junit-1-failure.xml");
    	assertTrue(build.getResult() == Result.UNSTABLE);
    	
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
    	assertTrue(build.getResult() == Result.UNSTABLE);
   	
    }

}
