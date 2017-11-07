package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.RootAction;
import hudson.model.Run;
import hudson.model.View;
import hudson.util.RunList;

import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.Stapler;

@Extension
public class ClaimedBuildsReport implements RootAction {

    public ClaimedBuildsReport() {
    }

    @Override
    public String getIconFileName() {
        return "/plugin/claim/icons/claim-24x24.png";
    }

    @Override
    public String getUrlName() {
        return "/claims";
    }

    public Run getFirstFail(Run r) {
        Run lastGood = r.getPreviousNotFailedBuild();
        Run firstFail;
        firstFail = lastGood == null ? r.getParent().getFirstBuild() : lastGood.getNextBuild();
        return firstFail;
    }

    public String getClaimantText(Run r) {
        ClaimBuildAction claim = r.getAction(ClaimBuildAction.class);
        if (claim == null || !claim.isClaimed()) {
            return Messages.ClaimedBuildsReport_ClaimantText_unclimed();
        }
        String reason = claim.getReason();
        if (reason != null) {
            return Messages.ClaimedBuildsReport_ClaimantText_claimedWithReason(
                claim.getClaimedBy(), claim.getReason(), claim.getAssignedBy());
        } else {
            return Messages.ClaimedBuildsReport_ClaimantText_claimed(claim
                                                                         .getClaimedBy(), claim.getAssignedBy());
        }
    }

    public View getOwner() {
        View view = Stapler.getCurrentRequest().findAncestorObject(View.class);
        return view != null ? view : Jenkins.getInstance().getStaplerFallback();
    }

    public RunList getBuilds() {
        List<Run> lastBuilds = new ArrayList<Run>();
        for (Job item : Jenkins.getInstance().getAllItems(Job.class)) {
            Job job = (Job) item;
            Run lb = job.getLastBuild();
            while (lb != null && (lb.hasntStartedYet() || lb.isBuilding()))
                lb = lb.getPreviousBuild();

            if (lb != null && lb.getAction(ClaimBuildAction.class) != null) {
                lastBuilds.add(lb);
            }
        }

        return RunList.fromRuns(lastBuilds).failureOnly();
    }

    @Override
    public String getDisplayName() {
        return Messages.ClaimedBuildsReport_DisplayName();
    }

}
