package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.TopLevelItem;
import hudson.model.TransientViewActionFactory;
import hudson.model.Api;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.View;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 9)
public class ClaimedBuildsReport implements Action {

    private View view;

    public ClaimedBuildsReport(View view) {
        this.view = view;
    }

    public String getIconFileName() {
        return "orange-square.gif";
    }

    public String getUrlName() {
        return "claims";
    }

    public static Run getFirstFail(Run r) {
        Run lastGood = r.getPreviousNotFailedBuild();
        Run firstFail;
        if (lastGood == null) {
            firstFail = r.getParent().getFirstBuild();
        } else {
            firstFail = lastGood.getNextBuild();
        }
        return firstFail;
    }

    public String getClaimantText(Run r) {
        ClaimBuildAction claim = r.getAction(ClaimBuildAction.class);
        if (claim == null || !claim.isClaimed()) {
            return "unclaimed";
        }
        String reason = claim.getReason();
        if (reason != null) {
            return "claimed by " + claim.getClaimedBy() + " because: "
                    + claim.getReason();
        } else {
            return "claimed by " + claim.getClaimedBy();
        }
    }

    public View getOwner() {
        return view;
    }

    public RunList getBuilds() {
        List<Run> lastBuilds = new ArrayList<Run>();
        for (TopLevelItem item : getOwner().getItems()) {
            if (item instanceof Job) {
                Job job = (Job) item;
                Run lb = job.getLastBuild();
                while (lb != null && (lb.hasntStartedYet() || lb.isBuilding()))
                    lb = lb.getPreviousBuild();

                if (lb != null && lb.getAction(ClaimBuildAction.class) != null) {
                    lastBuilds.add(lb);
                }

            }
        }

        return RunList.fromRuns(lastBuilds).failureOnly();
    }

    @Exported(name = "build")
    public List<ClaimReportEntry> getEntries() {
        List<ClaimReportEntry> entries = new ArrayList<ClaimedBuildsReport.ClaimReportEntry>();
        for (AbstractBuild r : (List<AbstractBuild>) getBuilds())
            entries.add(new ClaimReportEntry(r));
        return entries;
    }

    public String getDisplayName() {
        return "Claim Report";
    }

    public Api getApi() {
        return new Api(this);
    }

    @Extension
    public static class ClaimViewActionFactory extends TransientViewActionFactory {

        @Override
        public List<Action> createFor(View v) {
            return Collections
                    .<Action> singletonList(new ClaimedBuildsReport(v));
        }

    }

    @ExportedBean(defaultVisibility = 9)
    public static class ClaimReportEntry {
        private AbstractBuild<?, ?> run;

        public ClaimReportEntry(AbstractBuild<?, ?> run) {
            super();
            this.run = run;
        }

        @Exported
        public String getJob() {
            return run.getParent().getName();
        }

        @Exported
        public int getNumber() {
            return run.getNumber();
        }

        @Exported
        public ClaimBuildAction getClaim() {
            return run.getAction(ClaimBuildAction.class);
        }

        @Exported
        public String getFailingSince() {
            return getFirstFail(run).getTimestampString2();
        }

        @Exported
        public String getResult() {
            return run.getResult().toString();
        }

        @Exported
        public List<?> getFailedTests() {
            TestResultAction action = run.getAction(TestResultAction.class);
            if (action == null) return null;

            List<CaseResult> failedTests = action.getFailedTests();
            List<TestEntry> result = new ArrayList<TestEntry>(failedTests.size());
            for (CaseResult cr: failedTests) {
                TestEntry entry = new TestEntry();
                entry.test = cr;
                ClaimTestAction cta = cr.getTestAction(ClaimTestAction.class);
                if (cta != null) {
                    entry.claim = cta;
                }
                result.add(entry);
            }

            return result;

        }

    }

    @ExportedBean(defaultVisibility=9)
    public static class TestEntry {
        ClaimTestAction claim;
        CaseResult test;

        @Exported(inline=true)
        public CaseResult getTest() {
            return test;
        }


        @Exported
        public ClaimTestAction getClaim() {
            return claim;
        }

        @Exported
        public String getUrl() {
            return test.getOwner().getAbsoluteUrl() + "testReport/" + test.getUrl();
        }
    }

}