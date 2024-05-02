package hudson.plugins.claim;

import hudson.security.ACL;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestResult;
import jenkins.model.Jenkins;

/**
 * Represents a labeling action for test results, providing additional metadata.
 * Responsible for showing additional headers on the TestResult page.
 *
 * Jelly (see Junit Plugin):
 * <ul>
 * <li>casetableheader.jelly: allows additional table headers to be shown in tables that list test methods</li>
 * <li>classtableheader.jelly: allows additional table headers to be shown in tables that list test classes</li>
 * <li>packagetableheader.jelly: allows additional table headers to be shown in tables that list test packages</li>
 * <li>tablerow.jelly: allows additional table cells to be shown in tables that list test methods, classes and packages</li>
 * </ul>
 */
public class LabelTestAction extends TestAction {

    private final TestResult testResult;

    public LabelTestAction(TestResult testResult){
        this.testResult = testResult;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public TestResult getTestResult(){
        return testResult;
    }

    /**
     * Determines whether the current user is anonymous.
     *
     * @return true if the current user is anonymous, false otherwise.
     */
    public final boolean isUserAnonymous() {
        return ACL.isAnonymous2(Jenkins.getAuthentication2());
    }

}
