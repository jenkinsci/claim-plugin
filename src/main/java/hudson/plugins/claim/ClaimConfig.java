package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public final class ClaimConfig extends GlobalConfiguration {

    private static final String GROOVY_SCRIPT_KEY = "hudson.plugins.claim.ClaimConfig.groovyTrigger";

    @SuppressFBWarnings(
            value = {"NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"},
            justification = "groovyTrigger is initialized in setGroovyTrigger")
    public ClaimConfig() {
        load();
        if (groovyTrigger == null) {
            try {
                setGroovyTrigger(new SecureGroovyScript("", true, null), false);
            } catch (Descriptor.FormException e) {
                throw new IllegalStateException(
                        "This should never occur as sandbox is set to true, and the exception is thrown only if it is false.");
            }
        }
    }

    /**
     * Whether we want to send emails to the assignee when items are claimed/assigned.
     */
    private boolean sendEmails;

    /**
     * Whether we want to send emails to the assignee of sticky items failing again.
     */
    private boolean sendEmailsForStickyFailures;

    /**
     * Default global value for the stickiness of the build claims.
     */
    private boolean stickyByDefault = true;

    /**
     * Default global value for the propagation to following builds of the claims.
     */
    private boolean propagateToFollowingBuildsByDefault = false;

    /**
     * Sort users by full name.
     */
    private boolean sortUsersByFullName;

    /**
     * Display email address for assignees when claiming.
     */
    private boolean emailDisplayedForAssigneesList;

    /**
     * Groovy script to be run when a claim is changed.
     */
    @Deprecated
    private transient String groovyScript;

    @NonNull
    private SecureGroovyScript groovyTrigger;

    /**
     * This human readable name is used in the configuration screen.
     */
    @NonNull
    public String getDisplayName() {
        return "Claim";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

        // To persist global configuration information,
        // set that to properties and call save().
        sendEmails = formData.getBoolean("sendEmails");
        sendEmailsForStickyFailures = formData.getBoolean("sendEmailsForStickyFailures");
        stickyByDefault = formData.getBoolean("stickyByDefault");
        propagateToFollowingBuildsByDefault = formData.getBoolean("propagateToFollowingBuildsByDefault");
        sortUsersByFullName = formData.getBoolean("sortUsersByFullName");
        emailDisplayedForAssigneesList = formData.getBoolean("emailDisplayedForAssigneesList");
        setGroovyTrigger(req.bindJSON(SecureGroovyScript.class, formData.getJSONObject("groovyTrigger")));
        save();
        return super.configure(req, formData);
    }

    /**
     * This method returns true if the global configuration says we should send mails on initial claims.
     * @return true if configuration is that we send emails for initial claims, false otherwise
     */
    public boolean getSendEmails() {
        return sendEmails;
    }

    /**
     * Set whether we should send emails for initial claims.
     * @param val the setting to use
     */
    public void setSendEmails(boolean val) {
        sendEmails = val;
    }

    /**
     * This method returns true if the global configuration says we should send mails on build sticky failures.
     * @return true if configuration is that we send emails for sticky failures, false otherwise
     */
    public boolean getSendEmailsForStickyFailures() {
        return sendEmailsForStickyFailures;
    }

    /**
     * Set whether we should send emails for sticky failures.
     * @param val the setting to use
     */
    public void setSendEmailsForStickyFailures(boolean val) {
        sendEmailsForStickyFailures = val;
    }

    /**
     * Returns true if the claims should be sticky by default, false otherwise.
     *
     * @return true to make claims sticky by default, else false.
     */
    public boolean isStickyByDefault() {
        return stickyByDefault;
    }

    /**
     * Sets the default stickiness behaviour for build claims.
     *
     * @param stickyByDefault
     *            the default stickiness value.
     */
    public void setStickyByDefault(boolean stickyByDefault) {
        this.stickyByDefault = stickyByDefault;
    }

    /**
     * Returns true if the claims should be propagated to following builds by default, false otherwise.
     *
     * @return true to make claims propagated to following builds by default, else false.
     */
    public boolean isPropagateToFollowingBuildsByDefault() {
        return propagateToFollowingBuildsByDefault;
    }

    /**
     * Sets the default following builds propagation behaviour for claims.
     *
     * @param propagateToFollowingBuildsByDefault the default following build propagation value.
     */
    public void setPropagateToFollowingBuildsByDefault(boolean propagateToFollowingBuildsByDefault) {
        this.propagateToFollowingBuildsByDefault = propagateToFollowingBuildsByDefault;
    }

    /**
     * Returns true if the users should be sorted by full name instead of ids.
     *
     * @return true to make users sorted by full name, else false.
     */
    public boolean isSortUsersByFullName() {
        return sortUsersByFullName;
    }

    /**
     * Sets the user sort method.
     *
     * @param sortUsersByFullName
     *            true to make users sorted by full name, else false.
     */
    public void setSortUsersByFullName(boolean sortUsersByFullName) {
        this.sortUsersByFullName = sortUsersByFullName;
    }

    /**
     * Returns true if the email should be displayed in the assignee list when claiming.
     *
     * @return true to display email address, else false.
     */
    public boolean isEmailDisplayedForAssigneesList() {
        return emailDisplayedForAssigneesList;
    }

    /**
     * Sets the email display option for assignees list when claiming.
     *
     * @param emailDisplayedForAssigneesList true to display email address, else false.
     */
    public void setEmailDisplayedForAssigneesList(boolean emailDisplayedForAssigneesList) {
        this.emailDisplayedForAssigneesList = emailDisplayedForAssigneesList;
    }

    @NonNull
    public SecureGroovyScript getGroovyTrigger() {
        return groovyTrigger;
    }

    void setGroovyTrigger(@NonNull SecureGroovyScript groovyTrigger) {
        this.setGroovyTrigger(groovyTrigger, true);
    }

    private void setGroovyTrigger(@NonNull SecureGroovyScript trigger, boolean withCurrentUser) {
        ApprovalContext approvalContext = ApprovalContext.create().withKey(GROOVY_SCRIPT_KEY);
        if (withCurrentUser) {
            approvalContext = approvalContext.withCurrentUser();
        }
        this.groovyTrigger = trigger.configuring(approvalContext);
    }

    public boolean hasGroovyTrigger() {
        return StringUtils.isNotEmpty(groovyTrigger.getScript());
    }

    /**
     * Gets the current claim configuration.
     * @return the global claim configuration
     */
    public static ClaimConfig get() {
        return GlobalConfiguration.all().get(ClaimConfig.class);
    }

    @SuppressWarnings("deprecation")
    @SuppressFBWarnings(
            value = {"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"},
            justification = "during migration, field can be null")
    private Object readResolve() {
        // JENKINS-43811 migration logic
        String script;
        if (groovyTrigger == null) {
            if (groovyScript != null) {
                script = groovyScript;
            } else {
                script = "";
            }
            try {
                setGroovyTrigger(new SecureGroovyScript(script, true, null), false);
            } catch (Descriptor.FormException e) {
                throw new IllegalStateException(
                        "This should never occur as sandbox is set to true, and the exception is thrown only if it is false.");
            }
        }
        return this;
    }
}
