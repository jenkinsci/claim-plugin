package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

@Extension
public final class ClaimConfig extends GlobalConfiguration {

    private static final String GROOVY_SCRIPT_KEY = "hudson.plugins.claim.ClaimConfig.groovyTrigger";

    @SuppressFBWarnings(
            value = {"NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"},
            justification = "groovyTrigger is initialized in setGroovyTrigger")
    public ClaimConfig() {
        load();
        if (groovyTrigger == null) {
            setGroovyTrigger(new SecureGroovyScript("", true, null), false);
        }
    }

    /**
     * Whether we want to send emails to the assignee when items are claimed/assigned.
     */
    private boolean sendEmails;

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
     * Groovy script to be run when a claim is changed.
     */
    @Deprecated
    private transient String groovyScript;

    @Nonnull
    private SecureGroovyScript groovyTrigger;

    /**
     * Block Auto - refresh while claiming.
     */
    private boolean blockAutoRefreshWhileClaiming;

    /**
     * This human readable name is used in the configuration screen.
     */
    @Nonnull
    public String getDisplayName() {
        return "Claim";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

        // To persist global configuration information,
        // set that to properties and call save().
        sendEmails = formData.getBoolean("sendEmails");
        stickyByDefault = formData.getBoolean("stickyByDefault");
        propagateToFollowingBuildsByDefault = formData.getBoolean("propagateToFollowingBuildsByDefault");
        sortUsersByFullName = formData.getBoolean("sortUsersByFullName");
        blockAutoRefreshWhileClaiming = formData.getBoolean("blockAutoRefreshWhileClaiming");
        setGroovyTrigger(req.bindJSON(SecureGroovyScript.class, formData.getJSONObject("groovyTrigger")));
        save();
        return super.configure(req, formData);
    }

    /**
     * This method returns true if the global configuration says we should send mails on build claims.
     * @return true if configuration is that we send emails for claims, false otherwise
     */
    public boolean getSendEmails() {
        return sendEmails;
    }

    /**
     * Set whether we should send emails.
     * @param val the setting to use
     */
    public void setSendEmails(boolean val) {
        sendEmails = val;
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
     * Indicates if auto refresh is blocked while claiming.
     * @return true if auto refresh is blocked, else false;
     */
    public boolean isBlockAutoRefreshWhileClaiming() {
        return blockAutoRefreshWhileClaiming;
    }

    /**
     * Sets if auto refresh is blocked while claiming.
     * @param blockAutoRefreshWhileClaiming true if auto refresh is blocked, else false;
     */
    public void setBlockAutoRefreshWhileClaiming(boolean blockAutoRefreshWhileClaiming) {
        this.blockAutoRefreshWhileClaiming = blockAutoRefreshWhileClaiming;
    }

    @Nonnull
    public SecureGroovyScript getGroovyTrigger() {
        return groovyTrigger;
    }

    void setGroovyTrigger(@Nonnull SecureGroovyScript groovyTrigger) {
        this.setGroovyTrigger(groovyTrigger, true);
    }

    private void setGroovyTrigger(@Nonnull SecureGroovyScript trigger, boolean withCurrentUser) {
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
    private Object readResolve() {
        // JENKINS-43811 migration logic
        String script;
        if (groovyScript != null) {
            script = groovyScript;
        } else {
            script = "";
        }
        setGroovyTrigger(new SecureGroovyScript(script, true, null), false);
        return this;
    }
}
