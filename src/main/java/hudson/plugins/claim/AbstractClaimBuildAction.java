package hudson.plugins.claim;

import groovy.lang.Binding;
import hudson.model.*;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.verb.POST;

import jakarta.mail.MessagingException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    // jelly
    @POST
    public final void doClaim(StaplerRequest req, StaplerResponse resp)
            throws Exception {
        User currentUser = getCurrentUser();
        User claimedUser = currentUser; // Default to self-assignment
        String assignee = req.getSubmittedForm().getString("assignee");
        if (!StringUtils.isEmpty(assignee) && !claimedUser.equals(assignee)) {
            // Validate the specified assignee.
            User resolvedAssignee = getUserFromId(assignee, false);
            if (resolvedAssignee == null) {
                LOGGER.log(Level.WARNING, "Invalid username specified for assignment: {0}", assignee);
                resp.forwardToPreviousPage(req);
                return;
            }
            claimedUser = resolvedAssignee;
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
        boolean propagated = req.getSubmittedForm().getBoolean("propagateToFollowingBuilds");
        if (StringUtils.isEmpty(reasonProvided)) {
            reasonProvided = null;
        }
        claim(claimedUser, reasonProvided, currentUser, new Date(), sticky, propagated, true);
        this.getOwner().save();

        evalGroovyScript();
        resp.forwardToPreviousPage(req);
    }

    private static User getCurrentUser() {
        Authentication authentication = Jenkins.getAuthentication2();
        return User.get2(authentication);
    }

    /**
     * Claims a {@link Saveable}, and optionally notifies of the claim.
     * @param claimedByUser claiming user
     * @param providedReason reason for the claim
     * @param assignedByUser assigner user
     * @param date date of the claim
     * @param isSticky true if the claim has to be kept until resolution
     * @param isPropagated true if the claim has to be propagated to following builds
     * @param notify true if notifications have to be sent
     */
    public final void claim(User claimedByUser, String providedReason, User assignedByUser, Date date,
                            boolean isSticky, boolean isPropagated, boolean notify) {
        applyClaim(claimedByUser, providedReason, assignedByUser, date, isSticky, isPropagated);
        if (notify) {
            try {
                sendInitialClaimEmail(claimedByUser, providedReason, assignedByUser);
            } catch (IOException | MessagingException e) {
                LOGGER.log(Level.WARNING, "Exception encountered sending assignment email: " + e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted when sending assignment email", e);
            }
        }
    }

    /**
     * Sends an initial claim email.
     * @param claimedByUser the claiming user
     * @param providedReason reason for the claim
     * @param assignedByUser the assigner user
     * @throws MessagingException if there has been some problem with sending the email
     * @throws IOException if there is an IO problem when sending the mail
     * @throws InterruptedException if the send operation is interrupted
     */
    protected abstract void sendInitialClaimEmail(User claimedByUser, String providedReason, User assignedByUser)
            throws MessagingException, IOException, InterruptedException;

    /**
     * Applies the claim data to the {@link AbstractClaimBuildAction}.
     * @param claimedByUser the claiming user
     * @param providedReason reason for the claim
     * @param assignedByUser the assigner user
     * @param date date of the claim
     * @param isSticky true if the claim has to be kept until resolution
     * @param isPropagated true if the claim has to be propagated to following builds
     */
    protected void applyClaim(@Nonnull User claimedByUser, String providedReason, @Nonnull User assignedByUser, Date date,
                              boolean isSticky, boolean isPropagated) {
        this.claimed = true;
        this.claimedBy = claimedByUser.getId();
        this.reason = providedReason;
        this.transientClaim = !isSticky;
        this.claimDate = date;
        this.assignedBy = assignedByUser.getId();
        if (isPropagated) {
            getNextAction().ifPresent(action -> {
                if (!action.isClaimed()) {
                    action.applyClaim(claimedByUser, providedReason, assignedByUser, date, isSticky, true);
                    try {
                        action.getOwner().save();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });
        }
    }

    protected abstract Optional<AbstractClaimBuildAction> getNextAction();

    // jelly
    public final void doUnclaim(StaplerRequest req, StaplerResponse resp)
            throws ServletException, IOException {
        unclaim(false);

        if (ClaimBuildFailureAnalyzer.isBFAEnabled() && bfaClaimer != null) {
            bfaClaimer.removeFailAction((Run) getOwner());
        }
        getOwner().save();
        evalGroovyScript();
        resp.forwardToPreviousPage(req);
    }

    /**
     * Unclaims a {@link Saveable}, and optionally notifies of the unclaim.
     * @param notify true if notifications have to be sent
     */
    public final void unclaim(boolean notify) {
        //TODO actually notify
        applyUnclaim();
    }

    /**
     * Removes the claim data to the {@link AbstractClaimBuildAction}.
     */
    protected void applyUnclaim() {
        this.claimed = false;
        this.claimedBy = null;
        this.transientClaim = false;
        this.claimDate = null;
        this.assignedBy = null;
        // we remember the reason to show it if someone reclaims this build.
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
        User user = getUserFromId(claimedBy, false);
        if (user != null) {
            return user.getDisplayName();
        } else {
            return claimedBy;
        }
    }

    // used by groovy scripts ?
    public final String getAssignedByName() {
        User user = getUserFromId(assignedBy, false);
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
     * Claim a new {@link Saveable} with the same settings as this one.
     * @param other the source data
     * @return true if claim has been copied, false otherwise
     */
    protected boolean copyTo(AbstractClaimBuildAction<T> other) {
        User claimedBy = getUserFromId(getClaimedBy(), false);
        if (claimedBy == null)
        {
            return false;
        }
        User assignedBy = getUserFromId(getAssignedBy(), false);
        if (assignedBy == null)
        {
            assignedBy = User.getUnknown();
        }
        other.applyClaim(claimedBy, getReason(), assignedBy, getClaimDate(), isSticky(), false);
        return true;
    }

    public final boolean isClaimedByMe() {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            return currentUser.getId().equals(claimedBy);
        }
        return false;
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
        return ACL.isAnonymous2(Jenkins.getAuthentication2());
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

    @Restricted(DoNotUse.class)
    @SuppressWarnings("unused")
    // groovy
    public final boolean isPropagateToFollowingBuildsByDefault() {
        return ClaimConfig.get().isPropagateToFollowingBuildsByDefault();
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
                groovyScript.evaluate(Jenkins.get().getPluginManager().uberClassLoader, binding, TaskListener.NULL);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating Groovy script", e);
            }
        }
    }

    protected final User getUserFromId(String userId) {
        return getUserFromId(userId, true);
    }

    protected final User getUserFromId(String userId, boolean throwIfNotFound) {
        User resolved = User.get(userId, false, Collections.emptyMap());
        if (resolved == null && throwIfNotFound) {
            throw new UsernameNotFoundException("Unknown user: " + userId);
        }
        return resolved;
    }
}
