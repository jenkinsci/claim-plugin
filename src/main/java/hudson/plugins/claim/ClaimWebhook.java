package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A webhook that is notified when a test or build is claimed or unclaimed.
 */
public class ClaimWebhook extends AbstractDescribableImpl<ClaimWebhook> {

    static final int DEFAULT_TIMEOUT_SECONDS = 5;
    static final int MAX_TIMEOUT_SECONDS = 60;

    private final String url;
    private boolean enabled = true;
    private boolean onClaim = true;
    private boolean onUnclaim = true;
    private boolean forTests = true;
    private boolean forBuilds = true;
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private Secret secret;

    @DataBoundConstructor
    public ClaimWebhook(String url) {
        this.url = StringUtils.trimToEmpty(url);
    }

    public String getUrl() {
        return url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @DataBoundSetter
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOnClaim() {
        return onClaim;
    }

    @DataBoundSetter
    public void setOnClaim(boolean onClaim) {
        this.onClaim = onClaim;
    }

    public boolean isOnUnclaim() {
        return onUnclaim;
    }

    @DataBoundSetter
    public void setOnUnclaim(boolean onUnclaim) {
        this.onUnclaim = onUnclaim;
    }

    public boolean isForTests() {
        return forTests;
    }

    @DataBoundSetter
    public void setForTests(boolean forTests) {
        this.forTests = forTests;
    }

    public boolean isForBuilds() {
        return forBuilds;
    }

    @DataBoundSetter
    public void setForBuilds(boolean forBuilds) {
        this.forBuilds = forBuilds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @DataBoundSetter
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds <= 0
                ? DEFAULT_TIMEOUT_SECONDS
                : Math.min(timeoutSeconds, MAX_TIMEOUT_SECONDS);
    }

    @CheckForNull
    public Secret getSecret() {
        return secret;
    }

    @DataBoundSetter
    public void setSecret(Secret secret) {
        this.secret = secret != null && StringUtils.isNotEmpty(secret.getPlainText()) ? secret : null;
    }

    /**
     * Whether this webhook wants to be notified of the given event.
     * @param event the event
     * @param test true for test claims, false for build claims
     * @return true if the webhook is enabled, valid and subscribed to the event
     */
    boolean accepts(ClaimEvent event, boolean test) {
        if (!enabled || !isValidUrl(url)) {
            return false;
        }
        if (event == ClaimEvent.CLAIMED ? !onClaim : !onUnclaim) {
            return false;
        }
        return test ? forTests : forBuilds;
    }

    static boolean isValidUrl(String candidate) {
        if (StringUtils.isBlank(candidate)) {
            return false;
        }
        try {
            URI uri = new URI(candidate.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** The kind of event a webhook is notified about. */
    enum ClaimEvent {
        CLAIMED("claimed"),
        UNCLAIMED("unclaimed");

        private final String id;

        ClaimEvent(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ClaimWebhook> {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.ClaimWebhook_DisplayName();
        }

        @POST
        public FormValidation doCheckUrl(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.ClaimWebhook_UrlRequired());
            }
            if (!isValidUrl(value)) {
                return FormValidation.error(Messages.ClaimWebhook_UrlInvalid());
            }
            return FormValidation.ok();
        }

        /**
         * Sends a test event synchronously and reports the outcome. Never throws for delivery problems.
         */
        @POST
        public FormValidation doTestWebhook(@QueryParameter String url, @QueryParameter String secret,
                                            @QueryParameter int timeoutSeconds) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (!isValidUrl(url)) {
                return FormValidation.error(Messages.ClaimWebhook_UrlInvalid());
            }
            ClaimWebhook hook = new ClaimWebhook(url);
            hook.setSecret(Secret.fromString(secret));
            hook.setTimeoutSeconds(timeoutSeconds);
            JSONObject payload = new JSONObject();
            payload.put("event", "test");
            payload.put("message", "Test event from the Jenkins Claim plugin");
            payload.put("jenkinsUrl", Jenkins.get().getRootUrl());
            try {
                int status = ClaimWebhookNotifier.deliver(hook, payload.toString());
                if (status >= 200 && status < 300) {
                    return FormValidation.ok(Messages.ClaimWebhook_TestSuccess(status));
                }
                return FormValidation.warning(Messages.ClaimWebhook_TestHttpStatus(status));
            } catch (Exception e) {
                return FormValidation.error(Messages.ClaimWebhook_TestFailed(e.getMessage()));
            }
        }
    }
}
