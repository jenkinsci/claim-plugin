package hudson.plugins.claim;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;

public class ClaimTestDataPublisher extends TestDataPublisher {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");

    @DataBoundConstructor
    public ClaimTestDataPublisher() {
        // nothing to do
    }

    @Override
    public Data contributeTestData(Run<?, ?> run, @Nonnull FilePath workspace, Launcher launcher,
                                   TaskListener listener, TestResult testResult)
            throws IOException, InterruptedException {
        Data data = new Data(run);

        Map<String, List<CaseResult>> claimedFailuresByUser = new HashMap<>();
        for (CaseResult result: testResult.getFailedTests()) {
            CaseResult previous = result.getPreviousResult();
            if (previous != null) {
                ClaimTestAction previousAction = previous.getTestAction(ClaimTestAction.class);
                if (previousAction != null && previousAction.isClaimed() && previousAction.isSticky()) {
                    ClaimTestAction action = new ClaimTestAction(data, result.getId());
                    previousAction.copyTo(action);
                    data.addClaim(result.getId(), action);
                    putAsListElement(claimedFailuresByUser, action.getClaimedBy(), result);
                }
            }
        }

        sendEmailsForStickyFailuresIfPresent(run, testResult, claimedFailuresByUser);

        return data;
    }

    private <K, V> void putAsListElement(Map<K, List<V>> map, K key, V value) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(value);
        map.put(key, list);
    }

    private void sendEmailsForStickyFailuresIfPresent(Run run, TestResult testResult,
                                                      Map<String, List<CaseResult>> claimedFailuresByUser) {
        try {
            for (Entry<String, List<CaseResult>> entry : claimedFailuresByUser.entrySet()) {
                String url = Functions.joinPath(run.getUrl(), testResult.getParentAction().getUrlName());
                ClaimEmailer.sendRepeatedTestClaimEmailIfConfigured(
                    entry.getKey(), run.toString(), url, entry.getValue()
                );
            }
        } catch (MessagingException | IOException e) {
            LOGGER.log(Level.WARNING, "Exception when sending test failure reminder email. Ignoring.", e);
        }
    }

    public static final class Data extends TestResultAction.Data implements Saveable {

        private Map<String, ClaimTestAction> claims = new HashMap<>();

        private final Run<?, ?> build;

        public Data(Run<?, ?> build) {
            this.build = build;
        }

        public String getUrl() {
            return build.getUrl();
        }

        public Run<?, ?> getBuild() {
            return build;
        }

        @Override
        public List<? extends TestAction> getTestAction(
                @SuppressWarnings("deprecation") hudson.tasks.junit.TestObject testObject) {
            final String prefix = "junit";
            String id = testObject.getId();
            ClaimTestAction result = claims.get(id);

            // In Hudson 1.347 or so, IDs changed, and a junit/ prefix was added.
            // Attempt to fix this backward-incompatibility
            if (result == null && id.startsWith(prefix)) {
                result = claims.get(id.substring(prefix.length()));
            }

            if (result != null) {
                return Collections.singletonList(result);
            }

            if (testObject instanceof CaseResult) {
                CaseResult cr = (CaseResult) testObject;
                if (!cr.isPassed() && !cr.isSkipped()) {
                    return Collections.singletonList(new ClaimTestAction(this, id));
                }
            }

            return Collections.emptyList();
        }

        public void save() throws IOException {
            build.save();
        }

        public void addClaim(String testObjectId,
                ClaimTestAction claim) {
            claims.put(testObjectId, claim);
        }

    }

    @Extension
    @SuppressWarnings("unused")
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.ClaimTestDataPublisher_DisplayName();
        }
    }


}
