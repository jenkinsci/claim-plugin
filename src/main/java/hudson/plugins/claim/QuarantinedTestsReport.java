package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.RootAction;
import hudson.model.Run;
import hudson.model.View;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.History;
import hudson.tasks.test.TestResult;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class QuarantinedTestsReport implements RootAction {

    public QuarantinedTestsReport() {
    }

    public String getIconFileName() {
        return "/plugin/claim/icons/quarantine-24x24.png";
    }

    public String getUrlName() {
        return "/quarantine";
    }


    public View getOwner() {
    	StaplerRequest request = Stapler.getCurrentRequest();
    	if (request != null)
    	{
    		View view = request.findAncestorObject(View.class);
    		if (view != null) {
    			return view;
    		}
    	}
        return Hudson.getInstance().getView("All");
    }
    
    public QuarantineTestAction getAction(CaseResult test)
    {
    	return test.getTestAction(QuarantineTestAction.class);
    }

    public List<CaseResult> getQuarantinedTests() {
    	ArrayList<CaseResult> list = new ArrayList<CaseResult>();
        for (TopLevelItem item : getOwner().getItems()) {
            if (item instanceof Job) {
                Job job = (Job) item;
                Run lb = job.getLastBuild();
                while (lb != null && (lb.hasntStartedYet() || lb.isBuilding()))
                    lb = lb.getPreviousBuild();

                if (lb != null && lb.getAction(TestResultAction.class) != null) {
                	for (SuiteResult suite: lb.getAction(TestResultAction.class).getResult().getSuites())
                	{
                		for (CaseResult test: suite.getCases())
                		{
                			QuarantineTestAction action = test.getTestAction(QuarantineTestAction.class);
                			if (action != null && action.isQuarantined())
                			{
                				list.add(test);
                			}
                		}
                	}
                }
            }
        }
        return list;
    }
    
    public int getNumberOfSuccessivePasses(CaseResult test)
    {
    	int count = 0;
    	    	
    	for (TestResult result: test.getHistory().getList())
    	{
    		if (result.isPassed())
    		{
    			count++;
    		}
    		else
    		{
    			return count;
    		}
    	}
    	return count;
    }
   
    public String getDisplayName() {
        return Messages.QuarantinedTestsReport_DisplayName();
    }

}
