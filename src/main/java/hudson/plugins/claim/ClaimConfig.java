package hudson.plugins.claim;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

@Extension 
public class ClaimConfig extends GlobalConfiguration {

    public ClaimConfig() {
        load();
    }

    /**
     * Whether we want to send emails to the assignee when items are claimed/assigned
     */
    private boolean sendEmails;

    /**
     * Default global value for the stickiness of the build claims.
     */
    private boolean stickyByDefault = true;

    /**
     * Sort users by full name.
     */
    private boolean sortUsersByFullName;

    /**
     * Groovy script to be run when a claim is changed.
     */
    private String groovyScript;

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName() {
        return "Claim";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        // To persist global configuration information,
        // set that to properties and call save().
        sendEmails = formData.getBoolean("sendEmails");
        stickyByDefault = formData.getBoolean("stickyByDefault");
        sortUsersByFullName = formData.getBoolean("sortUsersByFullName");
        groovyScript = formData.getString("groovyScript");
        save();
        return super.configure(req,formData);
    }

    /**
     * This method returns true if the global configuration says we should send mails on build claims
     * @return true if configuration is that we send emails for claims, false otherwise
     */
    public boolean getSendEmails() {
        return sendEmails;
    }
    
    /**
     * Set whether we should send emails
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
     * This method returns the Groovy script as a String
     * @return String containing the Groovy script to run when claims are changed.
     */
    public String getGroovyScript() {
        return groovyScript;
    }
    
    /**
     * Set the Groovy script to run when a claim is changed.
     * @param val the script to use
     */
    public void setGroovyScript(String val) {
        groovyScript = val;
    }
    
    /**
     * get the current claim configuration
     * @return the global claim configuration
     */
    public static ClaimConfig get() {
        return GlobalConfiguration.all().get(ClaimConfig.class);
    }
}