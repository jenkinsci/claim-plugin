package hudson.plugins.claim;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.model.BuildBadgeAction;
import hudson.model.Describable;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;
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
    private String assignedBy;
    private Date claimDate;
    private boolean transientClaim = !ClaimConfig.get().isStickyByDefault();
    public static boolean isReclaim = false;
    private ClaimBuildFailureAnalyzer BFAClaimer = null;

    protected T owner;

    AbstractClaimBuildAction(T owner) {
        this.owner = owner;
        isReclaim = false;
    }

    private String reason;

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return "claim";
    }
    
    abstract String getUrl();

    public void doClaim(StaplerRequest req, StaplerResponse resp)
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
        String reason = req.getSubmittedForm().getString("reason");

        if(ClaimBuildFailureAnalyzer.isBFAEnabled()) {
            String error = req.getSubmittedForm().getString("errors");
            BFAClaimer = new ClaimBuildFailureAnalyzer(error);
            if (this.owner instanceof Run)
            {
                Run run = (Run) owner;
                if(!ClaimBuildFailureAnalyzer.ERROR.equals("Default")){
                    try{
                        BFAClaimer.createFailAction(run);
                    } catch (IndexOutOfBoundsException e){
                        LOGGER.log(Level.WARNING, "No FailureCauseBuildAction detected for this build");
                        resp.forwardToPreviousPage(req);
                        return;
                    }
                }
                else{
                    BFAClaimer.removeFailAction(run);
                }
            }
        }

        boolean sticky = req.getSubmittedForm().getBoolean("sticky");
        if (StringUtils.isEmpty(reason)) reason = null;
        claim(name, reason, currentUser, sticky);
        try {
            ClaimEmailer.sendEmailIfConfigured(User.get(name, false, Collections.EMPTY_MAP), currentUser, owner.toString(), reason, getUrl());
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "Exception encountered sending assignment email: " + e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted when sending assignment email",e);
        }
        isReclaim = true;
        owner.save();
        evalGroovyScript();
        resp.forwardToPreviousPage(req);
    }

    public void doUnclaim(StaplerRequest req, StaplerResponse resp)
            throws ServletException, IOException {
        unclaim();
        if(ClaimBuildFailureAnalyzer.isBFAEnabled() && BFAClaimer!=null)
            BFAClaimer.removeFailAction((Run) owner);
        isReclaim = false;
        owner.save();
        evalGroovyScript();
        resp.forwardToPreviousPage(req);
    }

    @Exported
    public String getClaimedBy() {
        return claimedBy;
    }
    
    @Exported
    public String getAssignedBy() {
    	return assignedBy;
    }

    public String getClaimedByName() {
        User user = User.get(claimedBy, false,Collections.EMPTY_MAP);
        if (user != null) {
            return user.getDisplayName();
        } else {
            return claimedBy;
        }
    }
    
    public String getAssignedByName() {
        User user = User.get(assignedBy, false,Collections.EMPTY_MAP);
        if (user != null) {
            return user.getDisplayName();
        } else {
            return assignedBy;
        }
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public void setAssignedBy (String assignedBy) {
    	this.assignedBy = assignedBy;
    }

    @Exported
    public boolean isClaimed() {
        return claimed;
    }

    public void claim(String claimedBy, String reason, String assignedBy, boolean sticky) {
        this.claimed = true;
        this.claimedBy = claimedBy;
        this.reason = reason;
        this.transientClaim = !sticky;
        this.claimDate = new Date();
        this.assignedBy = assignedBy;
    }

    /**
     * Claim a new Run with the same settings as this one.
     */
    public void copyTo(AbstractClaimBuildAction<T> other) {
        other.claim(getClaimedBy(), getReason(), getAssignedBy(), isSticky());
    }

    public void unclaim() {
        this.claimed = false;
        this.claimedBy = null;
        this.transientClaim = false;
        this.claimDate = null;
        this.assignedBy = null;
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

    public String fillReason() throws Exception {
        JSONObject json = new JSONObject();
        if(ClaimBuildFailureAnalyzer.isBFAEnabled()) {
            HashMap<String, String> map = ClaimBuildFailureAnalyzer.getFillReasonMap();
            for (String key : map.keySet()) {
                json.put(key, map.get(key));
            }
        }
        return json.toString();
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

    public String getError(){
        return ClaimBuildFailureAnalyzer.ERROR;
    }

    public boolean isBFAEnabled(){
        return ClaimBuildFailureAnalyzer.isBFAEnabled();
    }

    @Exported
    public Date getClaimDate() {
        return this.claimDate;
    }

    public boolean hasClaimDate() {
        return this.claimDate != null;
    }
    /**
     * was the action claimed by someone to themselves?
     * @return true if the item was claimed by the user to themselves, false otherwise 
     */
    public boolean isSelfAssigned() {
    	boolean ret = true;
    	if (! isClaimed()) {
    		ret = false;
    	} else if (getClaimedBy() == null) {
    		ret = false;
    	} else if (! getClaimedBy().equals(getAssignedBy())) {
    		ret = false;
    	}
    	return ret;
    }

    public abstract String getNoun();
    
    protected void evalGroovyScript() {
        ClaimConfig config = ClaimConfig.get();
        String groovyScript = config.getGroovyScript();
        if ((groovyScript != null) && (!groovyScript.isEmpty())) {
            Binding binding = new Binding();
            binding.setVariable("action", this);
            GroovyShell shell = new GroovyShell(binding);
            try {
                shell.evaluate(groovyScript);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating Groovy script",e);
            }
        }
    }
}
