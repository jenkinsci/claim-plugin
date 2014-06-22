package hudson.plugins.claim;

import hudson.model.BuildBadgeAction;
import hudson.model.Describable;
import hudson.model.ProminentProjectAction;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 2)
public abstract class AbstractClaimBuildAction<T extends Saveable> extends DescribableTestAction implements BuildBadgeAction,
        ProminentProjectAction, Describable<DescribableTestAction> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    private boolean claimed;
    private String claimedBy;
    private Date claimDate;
    private boolean transientClaim;

    protected T owner;

    AbstractClaimBuildAction(T owner) {
        this.owner = owner;
    }

    private String reason;

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return "claim";
    }

    public void doClaim(StaplerRequest req, StaplerResponse resp)
            throws ServletException, IOException {
        Authentication authentication = Hudson.getAuthentication();
        String name = authentication.getName(); // Default to self-assignment
        String assignee = req.getSubmittedForm().getString("assignee");
        if (!StringUtils.isEmpty(assignee) && !name.equals(assignee)) {
            // Validate the specified assignee.
            User resolvedAssignee = User.get(assignee, false, Collections.EMPTY_MAP);
            if (resolvedAssignee == null) {
                LOGGER.log(Level.WARNING, "Invalid username specified for assignment: {0}", assignee);
                resp.forwardToPreviousPage(req);
                return;
            }
            name = assignee;
        }
        String reason = req.getSubmittedForm().getString("reason");
        boolean sticky = req.getSubmittedForm().getBoolean("sticky");
        if (StringUtils.isEmpty(reason)) reason = null;
        claim(name, reason, sticky);
        owner.save();
        resp.forwardToPreviousPage(req);
    }

    public void doUnclaim(StaplerRequest req, StaplerResponse resp)
            throws ServletException, IOException {
        unclaim();
        owner.save();
        resp.forwardToPreviousPage(req);
    }

    @Exported
    public String getClaimedBy() {
        return claimedBy;
    }

    public String getClaimedByName() {
        User user = User.get(claimedBy, false,Collections.EMPTY_MAP);
        if (user != null) {
            return user.getDisplayName();
        } else {
            return claimedBy;
        }
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    @Exported
    public boolean isClaimed() {
        return claimed;
    }

    public void claim(String claimedBy, String reason, boolean sticky) {
        this.claimed = true;
        this.claimedBy = claimedBy;
        this.reason = reason;
        this.transientClaim = !sticky;
        this.claimDate = new Date();
    }

    /**
     * Claim a new Run with the same settings as this one.
     */
    public void copyTo(AbstractClaimBuildAction<T> other) {
        other.claim(getClaimedBy(), getReason(), isSticky());
    }

    public void unclaim() {
        this.claimed = false;
        this.claimedBy = null;
        this.transientClaim = false;
        this.claimDate = null;
        // we remember the reason to show it if someone reclaims this build.
    }

    public boolean isClaimedByMe() {
        return !isUserAnonymous()
                && Hudson.getAuthentication().getName().equals(claimedBy);
    }

    public boolean canClaim() {
        return !isUserAnonymous() && !isClaimedByMe();
    }

    public boolean canRelease() {
        return !isUserAnonymous() && isClaimedByMe();
    }

    public boolean isUserAnonymous() {
        return Hudson.getAuthentication().getName().equals("anonymous");
    }

    @Exported
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean hasReason() {
        return !StringUtils.isEmpty(reason);
    }

    public boolean isTransient() {
        return transientClaim;
    }

    public void setTransient(boolean transientClaim) {
        this.transientClaim = transientClaim;
    }

    public boolean isSticky() {
        return !transientClaim;
    }

    public void setSticky(boolean sticky) {
        this.transientClaim = !sticky;
    }

    @Exported
    public Date getClaimDate() {
        return this.claimDate;
    }

    public boolean hasClaimDate() {
        return this.claimDate != null;
    }

    public abstract String getNoun();

}
