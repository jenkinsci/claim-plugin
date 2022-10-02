package hudson.plugins.claim;

import hudson.model.*;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.Stapler;

import java.util.ArrayList;
import java.util.List;

public class AbstractAssignedClaimsReport implements Action, IconSpec {
    @Override
    public String getIconClassName() {
        return "symbol-solid/user-doctor plugin-font-awesome-api";
    }

    public String getIconFileName() {
        String iconClassName = getIconClassName();
        if (iconClassName != null) {
            Icon icon = IconSet.icons.getIconByClassSpec(iconClassName + " icon-md");
            if (icon != null) {
                JellyContext ctx = new JellyContext();
                ctx.setVariable("resURL", Stapler.getCurrentRequest().getContextPath() + Jenkins.RESOURCE_PATH);
                return icon.getQualifiedUrl(ctx);
            }
        }
        return null;
    }

    public String getUrlName() {
        return "/claims";
    }

    @Restricted(DoNotUse.class) // jelly only
    public Run getFirstFail(final Run r) {
        Run lastGood = r.getPreviousNotFailedBuild();
        Run firstFail;
        if (lastGood == null) {
            firstFail = r.getParent().getFirstBuild();
        } else {
            firstFail = lastGood.getNextBuild();
        }
        return firstFail;
    }

    @Restricted(DoNotUse.class) // jelly only
    public CommonMessagesProvider getMessageProvider(final Run r) {
        return CommonMessagesProvider.build(getAction(r));
    }

    @Restricted(DoNotUse.class) // jelly only
    public CommonMessagesProvider getMessageProvider(final TestResult tr) {
        return CommonMessagesProvider.build(tr.getTestAction(ClaimTestAction.class));
    }

    public ModelObject getOwner() {
        View view = Stapler.getCurrentRequest().findAncestorObject(View.class);
        if (view != null) {
            return view;
        } else {
            return Jenkins.getInstance().getStaplerFallback();
        }
    }

    private ClaimBuildAction getAction(final Run r) {
        return ClaimUtils.getBuildAction(r, false);
    }

    public RunList getBuilds() {
        List<Run> lastBuilds = new ArrayList<>();
        for (Job job : getJobs()) {
            Run lb = job.getLastCompletedBuild();
            if (lb != null && isDisplayed(lb.getAction(ClaimBuildAction.class))) {
                lastBuilds.add(lb);
            }
        }

        return RunList.fromRuns(lastBuilds).failureOnly();
    }

    protected List<Job> getJobs() {
        return Jenkins.getInstance().getAllItems(Job.class);
    }

    public List<TestResult> getTestFailuresWithClaim() {
        List<TestResult> claimedTestFailures = new ArrayList<>();
        for (Job job : getJobs()) {
            Run lb = job.getLastCompletedBuild();
            if (lb != null) {
                AbstractTestResultAction testResultAction = lb.getAction(AbstractTestResultAction.class);
                if (testResultAction != null) {
                    List<? extends TestResult> failedTests = testResultAction.getFailedTests();
                    for (TestResult failedTest : failedTests) {
                        ClaimTestAction claimAction = failedTest.getTestAction(ClaimTestAction.class);
                        if (isDisplayed(claimAction)) {
                            claimedTestFailures.add(failedTest);
                        }
                    }
                }
            }
        }

        return claimedTestFailures;
    }
    
    protected boolean isDisplayed(AbstractClaimBuildAction<?> claimAction) {
        return claimAction != null && claimAction.isClaimed();
    }

    public String getDisplayName() {
        return Messages.ClaimedBuildsReport_DisplayName();
    }

}
