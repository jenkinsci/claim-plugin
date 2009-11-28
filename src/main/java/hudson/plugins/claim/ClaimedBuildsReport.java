package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.RootAction;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.Stapler;

@Extension
public class ClaimedBuildsReport implements RootAction {

    public ClaimedBuildsReport() {
    }

    public String getIconFileName() {
        return "orange-square.gif";
    }

    public String getUrlName() {
        return "/claims";
    }

    public Run getFirstFail(Run r) {
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
        View view = Stapler.getCurrentRequest().findAncestorObject(View.class);
        if (view != null) {
            return view;
        } else {
            return Hudson.getInstance().getPrimaryView();
        }
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

    public String getDisplayName() {
        return "Claim Report";
    }

}
