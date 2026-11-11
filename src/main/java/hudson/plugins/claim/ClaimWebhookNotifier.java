package hudson.plugins.claim;

import hudson.ProxyConfiguration;
import hudson.model.Run;
import hudson.model.User;
import hudson.util.Secret;
import hudson.plugins.claim.ClaimWebhook.ClaimEvent;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Notifies the configured {@link ClaimWebhook}s about claim changes.
 * <p>
 * Delivery is asynchronous and best effort: no failure here may ever affect the claim itself.
 */
final class ClaimWebhookNotifier {

    static final String SIGNATURE_HEADER = "X-Claim-Signature";
    static final String EVENT_HEADER = "X-Claim-Event";

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");
    private static final int QUEUE_CAPACITY = 100;

    private static final ThreadPoolExecutor EXECUTOR = createExecutor();

    private ClaimWebhookNotifier() {
    }

    private static ThreadPoolExecutor createExecutor() {
        ThreadFactory factory = runnable -> {
            Thread t = new Thread(runnable, "claim-webhook");
            t.setDaemon(true);
            return t;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 2, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY), factory);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * Notifies all matching webhooks. Never throws.
     */
    static void notify(ClaimEvent event, AbstractClaimBuildAction<?> action, User previousClaimant,
                       User triggeredBy, String reason, boolean sticky, boolean propagated) {
        try {
            ClaimConfig config = ClaimConfig.get();
            if (config == null) {
                return;
            }
            boolean test = action instanceof ClaimTestAction;
            List<ClaimWebhook> hooks = config.getWebhooks().stream()
                    .filter(h -> h.accepts(event, test))
                    .toList();
            if (hooks.isEmpty()) {
                return;
            }
            String body = buildPayload(event, action, test, previousClaimant, triggeredBy, reason, sticky, propagated)
                    .toString();
            for (ClaimWebhook hook : hooks) {
                submit(hook, event, body);
            }
        } catch (Throwable t) { // NOSONAR: a webhook problem must never break claiming
            LOGGER.log(Level.WARNING, "Could not prepare claim webhook notification", t);
        }
    }

    private static void submit(ClaimWebhook hook, ClaimEvent event, String body) {
        try {
            EXECUTOR.execute(() -> {
                try {
                    int status = deliver(hook, body, event.getId());
                    if (status < 200 || status >= 300) {
                        LOGGER.log(Level.WARNING, "Claim webhook {0} answered with HTTP {1}",
                                new Object[] {hook.getUrl(), status});
                    }
                } catch (Throwable t) { // NOSONAR
                    LOGGER.log(Level.WARNING, "Claim webhook " + hook.getUrl() + " could not be notified: "
                            + t.getMessage(), t);
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.log(Level.WARNING, "Claim webhook queue is full, dropping event for {0}", hook.getUrl());
        }
    }

    static int deliver(ClaimWebhook hook, String body) throws IOException {
        return deliver(hook, body, "test");
    }

    static int deliver(ClaimWebhook hook, String body, String eventId) throws IOException {
        int timeoutMs = (int) TimeUnit.SECONDS.toMillis(hook.getTimeoutSeconds());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = (HttpURLConnection) ProxyConfiguration.open(java.net.URI.create(hook.getUrl()).toURL());
        if (con == null) {
            throw new IOException("Could not open connection to " + hook.getUrl());
        }
        try {
            con.setConnectTimeout(timeoutMs);
            con.setReadTimeout(timeoutMs);
            con.setInstanceFollowRedirects(false);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("User-Agent", "Jenkins-Claim-Plugin");
            con.setRequestProperty(EVENT_HEADER, eventId);
            Secret secret = hook.getSecret();
            if (secret != null) {
                con.setRequestProperty(SIGNATURE_HEADER, "sha256=" + sign(secret.getPlainText(), bytes));
            }
            con.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = con.getOutputStream()) {
                out.write(bytes);
            }
            return con.getResponseCode();
        } finally {
            con.disconnect();
        }
    }

    static String sign(String secret, byte[] payload) throws IOException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IOException("Could not sign payload", e);
        }
    }

    static JSONObject buildPayload(ClaimEvent event, AbstractClaimBuildAction<?> action, boolean test,
                                   User previousClaimant, User triggeredBy, String reason, boolean sticky,
                                   boolean propagated) {
        JSONObject json = new JSONObject();
        json.put("event", event.getId());
        json.put("type", test ? "test" : "build");

        Object owner = action.getOwner();
        String rootUrl = StringUtils.defaultString(Jenkins.get().getRootUrl());
        json.put("jenkinsUrl", rootUrl);
        if (owner instanceof Run<?, ?> run) {
            json.put("job", run.getParent().getFullName());
            json.put("build", run.getNumber());
            json.put("buildUrl", rootUrl + run.getUrl());
        }
        if (action instanceof ClaimTestAction testAction) {
            json.put("test", testAction.getTestObjectId());
            json.put("testUrl", rootUrl + action.getUrl());
        }
        json.put("claimedBy", userJson(previousClaimant));
        json.put("triggeredBy", userJson(triggeredBy));
        json.put("reason", reason);
        json.put("sticky", sticky);
        json.put("propagated", propagated);
        json.put("timestamp", Instant.ofEpochMilli(new Date().getTime()).toString());
        return json;
    }

    private static Object userJson(User user) {
        if (user == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        json.put("id", user.getId());
        json.put("name", user.getDisplayName());
        return json;
    }
}
