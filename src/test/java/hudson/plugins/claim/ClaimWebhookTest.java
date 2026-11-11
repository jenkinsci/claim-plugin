package hudson.plugins.claim;

import com.sun.net.httpserver.HttpServer;
import hudson.model.Build;
import hudson.model.Project;
import hudson.model.User;
import hudson.plugins.claim.utils.TestBuilder;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class ClaimWebhookTest {

    private JenkinsRule j;
    private HttpServer server;
    private final BlockingQueue<String> bodies = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> signatures = new LinkedBlockingQueue<>();
    private volatile int status = 200;
    private Build<?, ?> build;
    private ClaimBuildAction action;
    private User user;

    @BeforeEach
    void setUp(JenkinsRule j) throws Exception {
        this.j = j;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String sig = exchange.getRequestHeaders().getFirst(ClaimWebhookNotifier.SIGNATURE_HEADER);
            signatures.add(sig == null ? "" : sig);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();

        Project<?, ?> project = j.createFreeStyleProject("x");
        project.getBuildersList().add(new TestBuilder());
        project.getPublishersList().add(new ClaimPublisher());
        build = project.scheduleBuild2(0).get();
        action = build.getAction(ClaimBuildAction.class);
        user = User.getById("alice", true);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
    }

    private ClaimWebhook configure(ClaimWebhook hook) {
        ClaimConfig.get().setWebhooks(List.of(hook));
        return hook;
    }

    @Test
    void claimAndUnclaimAreSent() throws Exception {
        configure(new ClaimWebhook(url()));
        action.claim(user, "because", user, new Date(), true, false, false);
        JSONObject claimed = JSONObject.fromObject(bodies.poll(10, TimeUnit.SECONDS));
        assertEquals("claimed", claimed.getString("event"));
        assertEquals("build", claimed.getString("type"));
        assertEquals("x", claimed.getString("job"));
        assertEquals("alice", claimed.getJSONObject("claimedBy").getString("id"));
        assertEquals("because", claimed.getString("reason"));

        action.unclaim(false);
        JSONObject unclaimed = JSONObject.fromObject(bodies.poll(10, TimeUnit.SECONDS));
        assertEquals("unclaimed", unclaimed.getString("event"));
        assertEquals("alice", unclaimed.getJSONObject("claimedBy").getString("id"));
    }

    @Test
    void filtersAndDisabledHooksSendNothing() throws Exception {
        ClaimWebhook hook = new ClaimWebhook(url());
        hook.setOnClaim(false);
        configure(hook);
        action.claim(user, null, user, new Date(), true, false, false);
        assertNull(bodies.poll(1, TimeUnit.SECONDS));

        ClaimWebhook onlyTests = new ClaimWebhook(url());
        onlyTests.setForBuilds(false);
        configure(onlyTests);
        action.unclaim(false);
        assertNull(bodies.poll(1, TimeUnit.SECONDS));

        ClaimWebhook disabled = new ClaimWebhook(url());
        disabled.setEnabled(false);
        configure(disabled);
        action.claim(user, null, user, new Date(), true, false, false);
        assertNull(bodies.poll(1, TimeUnit.SECONDS));
    }

    @Test
    void payloadIsSignedWhenSecretIsSet() throws Exception {
        ClaimWebhook hook = new ClaimWebhook(url());
        hook.setSecret(Secret.fromString("s3cret"));
        configure(hook);
        action.claim(user, null, user, new Date(), true, false, false);
        String body = bodies.poll(10, TimeUnit.SECONDS);
        String sig = signatures.poll(10, TimeUnit.SECONDS);
        assertNotNull(body);
        assertEquals("sha256=" + ClaimWebhookNotifier.sign("s3cret", body.getBytes(StandardCharsets.UTF_8)), sig);
    }

    @Test
    void failingHooksNeverBreakClaiming() throws Exception {
        status = 500;
        configure(new ClaimWebhook(url()));
        action.claim(user, null, user, new Date(), true, false, false);
        assertNotNull(bodies.poll(10, TimeUnit.SECONDS));
        assertTrue(action.isClaimed());

        // unreachable host and invalid URL
        server.stop(0);
        ClaimConfig.get().setWebhooks(List.of(new ClaimWebhook(url()), new ClaimWebhook("not a url")));
        action.unclaim(false);
        assertFalse(action.isClaimed());
        action.claim(user, null, user, new Date(), true, false, false);
        assertTrue(action.isClaimed());
    }

    @Test
    void configurationSurvivesRoundtrip() throws Exception {
        ClaimWebhook hook = new ClaimWebhook("https://example.org/hook");
        hook.setOnUnclaim(false);
        hook.setTimeoutSeconds(7);
        configure(hook);
        j.configRoundtrip();
        List<ClaimWebhook> hooks = ClaimConfig.get().getWebhooks();
        assertEquals(1, hooks.size());
        assertEquals("https://example.org/hook", hooks.get(0).getUrl());
        assertFalse(hooks.get(0).isOnUnclaim());
        assertTrue(hooks.get(0).isOnClaim());
        assertEquals(7, hooks.get(0).getTimeoutSeconds());
    }
}
