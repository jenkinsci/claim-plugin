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

import org.apache.commons.jelly.JellyContext;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.stapler.Stapler;

@Extension
public class ClaimedBuildsReport implements RootAction, IconSpec {

    public ClaimedBuildsReport() {
    }

    @Override
    public String getIconClassName() {
        return "icon-claim-claim";
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
        if (view != null) {
            return view;
        } else {
            return Jenkins.getInstance().getStaplerFallback();
        }
    }

    public RunList getBuilds() {
        List<Run> lastBuilds = new ArrayList<>();
        for (Job job : Jenkins.getInstance().getAllItems(Job.class)) {
            Run lb = job.getLastBuild();
            while (lb != null && (lb.hasntStartedYet() || lb.isBuilding()))
                lb = lb.getPreviousBuild();

            if (lb != null && lb.getAction(ClaimBuildAction.class) != null) {
                lastBuilds.add(lb);
            }
        }

        return RunList.fromRuns(lastBuilds).failureOnly();
    }

    public String getDisplayName() {
        return Messages.ClaimedBuildsReport_DisplayName();
    }

}
