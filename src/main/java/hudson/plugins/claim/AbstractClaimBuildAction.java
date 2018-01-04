package hudson.plugins.claim;

import groovy.lang.Binding;
import hudson.model.BuildBadgeAction;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.servlet.ServletException;

import hudson.plugins.claim.http.PreventRefreshFilter;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 2)
public abstract class AbstractClaimBuildAction<T extends Saveable>
        extends DescribableTestAction
        implements BuildBadgeAction, ProminentProjectAction {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    private boolean claimed;
    private String claimedBy;
    private String assignedBy;
    private Date claimDate;
    private boolean transientClaim = !ClaimConfig.get().isStickyByDefault();
    private ClaimBuildFailureAnalyzer bfaClaimer = null;
    private String reason;

    protected abstract T getOwner();

    AbstractClaimBuildAction() {
    }

    // jelly
    public final CommonMessagesProvider getMessageProvider() {
        return CommonMessagesProvider.build(this);
    }

    /**
     * Indicates if the {@link Saveable} is claimed.
     *
     * @deprecated use {@link #isClaimed()} instead
     * @return true if the {@link Saveable} is claimed, else false
     */
    @Deprecated
    public final boolean isReclaim() {
        return isClaimed();
    }

    public final ClaimBuildFailureAnalyzer getBfaClaimer() {
        return bfaClaimer;
    }

    public final String getIconFileName() {
        return null;
    }

    public final String getUrlName() {
        return "claim";
    }

    abstract String getUrl();

    public final void doClaim(StaplerRequest req, StaplerResponse resp)
            throws Exception {
        Authentication authentication = Hudson.getAuthentication();
        String currentUser = authentication.getName();
        String name = currentUser; // Default to self-assignment
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
        String reasonProvided = req.getSubmittedForm().getString("reason");

        if (ClaimBuildFailureAnalyzer.isBFAEnabled()) {
            String error = req.getSubmittedForm().getString("errors");
            bfaClaimer = new ClaimBuildFailureAnalyzer(error);
            if (getOwner() instanceof Run) {
                Run run = (Run) getOwner();
                if (!bfaClaimer.isDefaultError()) {
                    try {
                        bfaClaimer.createFailAction(run);
                    } catch (IndexOutOfBoundsException e) {
                        LOGGER.log(Level.WARNING, "No FailureCauseBuildAction detected for this build");
                        resp.forwardToPreviousPage(req);
                        return;
                    }
                } else {
                    bfaClaimer.removeFailAction(run);
                }
            }
        }

        boolean sticky = req.getSubmittedForm().getBoolean("sticky");
        if (StringUtils.isEmpty(reasonProvided)) {
            reasonProvided = null;
        }
        claim(name, reasonProvided, currentUser, sticky);
        try {
            ClaimEmailer.sendEmailIfConfigured(
                    User.get(name, false, Collections.EMPTY_MAP),
                    currentUser,
                    getOwner().toString(),
                    reasonProvided,
                    getUrl());
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "Exception encountered sending assignment email: " + e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted when sending assignment email", e);
        }
        this.getOwner().save();
        evalGroovyScript();
        resp.forwardToPreviousPage(req);
    }

    // jelly
    public final void doUnclaim(StaplerRequest req, StaplerResponse resp)
            throws ServletException, IOException {
        unclaim();

        if (ClaimBuildFailureAnalyzer.isBFAEnabled() && bfaClaimer != null) {
            bfaClaimer.removeFailAction((Run) getOwner());
        }
        getOwner().save();
        evalGroovyScript();
        resp.forwardToPreviousPage(req);
    }

    @Exported
    public final String getClaimedBy() {
        return claimedBy;
    }

    @Exported
    public final String getAssignedBy() {
        return assignedBy;
    }

    // used by groovy scripts ?
    public final String getClaimedByName() {
        User user = User.get(claimedBy, false, Collections.EMPTY_MAP);
        if (user != null) {
            return user.getDisplayName();
        } else {
            return claimedBy;
        }
    }

    // used by groovy scripts ?
    public final String getAssignedByName() {
        User user = User.get(assignedBy, false, Collections.EMPTY_MAP);
        if (user != null) {
            return user.getDisplayName();
        } else {
            return assignedBy;
        }
    }

    // used by groovy scripts ?
    public final void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    // used by groovy scripts ?
    public final void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    @Exported
    public final boolean isClaimed() {
        return claimed;
    }

    /**
     * Claims a {@link Saveable}.
     * @param claimUser name of the claiming user
     * @param providedReason reason for the claim
     * @param assignedUser name of the assigned user
     * @param isSticky true if the claim has to be kept until resolution
     */
    public void claim(String claimUser, String providedReason, String assignedUser, boolean isSticky) {
        this.claimed = true;
        this.claimedBy = claimUser;
        this.reason = providedReason;
        this.transientClaim = !isSticky;
        this.claimDate = new Date();
        this.assignedBy = assignedUser;
    }

    /**
     * Claim a new {@link Saveable} with the same settings as this one.
     * @param other the source data
     */
    public void copyTo(AbstractClaimBuildAction<T> other) {
        other.claim(getClaimedBy(), getReason(), getAssignedBy(), isSticky());
    }

    public final void unclaim() {
        this.claimed = false;
        this.claimedBy = null;
        this.transientClaim = false;
        this.claimDate = null;
        this.assignedBy = null;
        // we remember the reason to show it if someone reclaims this build.
    }

    public final boolean isClaimedByMe() {
        return !isUserAnonymous()
                && Hudson.getAuthentication().getName().equals(claimedBy);
    }

    // jelly
    public final boolean canReassign() {
        return !isUserAnonymous() && isClaimed();
    }

    // jelly
    public final boolean canClaim() {
        return !isUserAnonymous() && !isClaimedByMe();
    }

    // jelly
    public final boolean canRelease() {
        return !isUserAnonymous() && isClaimedByMe();
    }

    public final boolean isUserAnonymous() {
        return Hudson.getAuthentication().getName().equals("anonymous");
    }

    @Exported
    public final String getReason() {
        return reason;
    }

    @JavaScriptMethod
    public final String getReason(String error) throws Exception {
        final String defaultValue = "";
        if (!ClaimBuildFailureAnalyzer.isBFAEnabled()) {
            return defaultValue;
        }
        if (error == null || ClaimBuildFailureAnalyzer.DEFAULT_ERROR.equals(error)) {
            return defaultValue;
        }
        return ClaimBuildFailureAnalyzer.getFillReasonMap().getOrDefault(error, defaultValue);
    }

    @Restricted(DoNotUse.class) // jelly
    @SuppressWarnings("unused")
    public final void preventRefresh(StaplerResponse response) {
        PreventRefreshFilter.preventRefresh(response);
    }

    // used by groovy scripts ?
    public final void setReason(String reason) {
        this.reason = reason;
    }

    // jelly
    public final boolean hasReason() {
        return !StringUtils.isEmpty(reason);
    }

    // used by groovy scripts ?
    public final boolean isTransientClaim() {
        return transientClaim;
    }

    // used by groovy scripts ?
    public final void setTransientClaim(boolean transientClaim) {
        this.transientClaim = transientClaim;
    }

    // used by groovy scripts ?
    public final boolean isSticky() {
        return !transientClaim;
    }

    // used by groovy scripts ?
    public final void setSticky(boolean sticky) {
        this.transientClaim = !sticky;
    }

    // used by groovy scripts ?
    public final String getError() {
        if (bfaClaimer == null) {
            return null;
        }
        return bfaClaimer.getError();
    }

    // used by groovy scripts ?
    public final boolean isBFAEnabled() {
        return ClaimBuildFailureAnalyzer.isBFAEnabled();
    }

    @Exported
    public final Date getClaimDate() {
        if (this.claimDate == null) {
            return null;
        }
        return (Date) this.claimDate.clone();
    }

    // used by groovy scripts ?
    public final  boolean hasClaimDate() {
        return this.claimDate != null;
    }

    /**
     * Was the action claimed by someone to themselves?
     * @return true if the item was claimed by the user to themselves, false otherwise
     */
    // used by groovy scripts ?
    public boolean isSelfAssigned() {
        boolean ret = true;
        if (!isClaimed()) {
            ret = false;
        } else if (getClaimedBy() == null) {
            ret = false;
        } else if (!getClaimedBy().equals(getAssignedBy())) {
            ret = false;
        }
        return ret;
    }

    // jelly
    public abstract String getNoun();

    protected final void evalGroovyScript() {
        ClaimConfig config = ClaimConfig.get();
        if (config.hasGroovyTrigger()) {
            SecureGroovyScript groovyScript = config.getGroovyTrigger();
            Binding binding = new Binding();
            binding.setVariable("action", this);
            try {
                groovyScript.evaluate(Jenkins.getInstance().getPluginManager().uberClassLoader, binding);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating Groovy script", e);
            }
        }
    }
}
