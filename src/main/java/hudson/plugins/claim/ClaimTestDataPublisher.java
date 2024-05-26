package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.junit.*;
import hudson.tasks.test.TabulatedResult;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import jakarta.mail.MessagingException;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClaimTestDataPublisher extends TestDataPublisher {

    private static final Logger LOGGER = Logger.getLogger("claim-plugin");
    private boolean displayClaimActionsInTestResultsTable;

    @DataBoundConstructor
    public ClaimTestDataPublisher(boolean displayClaimActionsInTestResultsTable) {
        // nothing to do
        this.displayClaimActionsInTestResultsTable = displayClaimActionsInTestResultsTable;
    }

    public boolean isDisplayClaimActionsInTestResultsTable() {
        return displayClaimActionsInTestResultsTable;
    }

    public void setDisplayClaimActionsInTestResultsTable(boolean displayClaimActionsInTestResultsTable) {
        this.displayClaimActionsInTestResultsTable = displayClaimActionsInTestResultsTable;
    }

    @Override
    public Data contributeTestData(Run<?, ?> run, @NonNull FilePath workspace, Launcher launcher,
                                   TaskListener listener, TestResult testResult)
            throws IOException, InterruptedException {
        Data data = new Data(run, displayClaimActionsInTestResultsTable);

        Map<User, List<CaseResult>> claimedFailuresByUser = new HashMap<>();
        for (CaseResult result: testResult.getFailedTests()) {
            CaseResult previous = result.getPreviousResult();
            if (previous != null) {
                ClaimTestAction previousAction = previous.getTestAction(ClaimTestAction.class);
                if (previousAction != null && previousAction.isClaimed() && previousAction.isSticky()) {
                    ClaimTestAction action = new ClaimTestAction(data, result.getId());
                    if (previousAction.copyTo(action)) {
                        data.addClaim(result.getId(), action);
                        User user = action.getUserFromId(action.getClaimedBy());
                        putAsListElement(claimedFailuresByUser, user, result);
                    }
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
                                                      Map<User, List<CaseResult>> claimedFailuresByUser) {
        try {
            for (Entry<User, List<CaseResult>> entry : claimedFailuresByUser.entrySet()) {
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
        private final boolean displayClaimActionsInTestResultsTable;

        public Data(Run<?, ?> build, boolean displayClaimActionsInTestResultsTable) {
            this.build = build;
            this.displayClaimActionsInTestResultsTable = displayClaimActionsInTestResultsTable;
        }

        public boolean isDisplayClaimActionsInTestResultsTable() {
            return displayClaimActionsInTestResultsTable;
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
                } else {
                    return Collections.singletonList(new LabelTestAction(this));
                }
            }
            if (testObject instanceof TestResult || testObject instanceof ClassResult || testObject instanceof PackageResult){
                return Collections.singletonList(new LabelTestAction(this));
            }

            return Collections.emptyList();
        }

        public void save() throws IOException {
            build.save();
        }

        public void addClaim(String testObjectId, ClaimTestAction claim) {
            claims.put(testObjectId, claim);
        }

    }

    @Extension
    @SuppressWarnings("unused")
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.ClaimTestDataPublisher_DisplayName();
        }
    }
}
