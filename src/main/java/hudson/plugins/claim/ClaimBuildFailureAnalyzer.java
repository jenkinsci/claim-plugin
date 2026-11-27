package hudson.plugins.claim;

import com.google.common.collect.Lists;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.StatisticsLogger;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.Plugin;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClaimBuildFailureAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(ClaimBuildFailureAnalyzer.class.getName());

    public static final String DEFAULT_ERROR = "Default";
    private static final String MATCHING_FILE = "Claim";

    @NonNull
    private final String error;

    public ClaimBuildFailureAnalyzer(@NonNull String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public boolean isDefaultError() {
        return DEFAULT_ERROR.equals(error);
    }

    public static Collection<FailureCause> getFailureCauses() throws Exception {
        return getPluginImpl().getKnowledgeBase().getCauses();
    }

    private static PluginImpl getPluginImpl() {
        return ExtensionList.lookupSingleton(PluginImpl.class);
    }

    public static boolean isBFAEnabled() {
        Plugin plugin = Jenkins.get().getPlugin("build-failure-analyzer");
        return plugin != null && plugin.getWrapper().isActive() && getPluginImpl().isGlobalEnabled();
    }

    public static HashMap<String, String> getFillReasonMap() throws Exception {
        HashMap<String, String> map = new HashMap<>();
        for (FailureCause cause : getFailureCauses()) {
            map.put(cause.getName(), cause.getDescription());
        }
        return map;
    }

    public static LinkedList<String> getDropdownList() throws Exception {
        LinkedList<String> list = new LinkedList<>();
        for (FailureCause cause : getFailureCauses()) {
            list.add(cause.getName());
        }
        return list;
    }

    public void createFailAction(Run run) throws Exception {
        FoundFailureCause newClaimedFailureCause = null;
        List<FoundIndication> indications = new LinkedList<>();
        for (FailureCause cause : getFailureCauses()) {
            if (cause.getName().equals(error)) {
                indications.add(new ClaimIndication(run, "Null", MATCHING_FILE, "Null"));
                newClaimedFailureCause = new FoundFailureCause(cause, indications);
                break;
            }
        }
        if (newClaimedFailureCause == null) {
            LOGGER.log(Level.WARNING, "Unknown failure cause \"{0}\", nothing to claim", error);
            return;
        }

        getPluginImpl().save();

        List<FailureCauseBuildAction> bfaActionList = run.getActions(FailureCauseBuildAction.class);
        FoundFailureCause existingClaimedFoundFailureCause = null;
        FailureCauseBuildAction bfaAction = bfaActionList.get(0);
        List<FoundFailureCause> foundFailureCauses = bfaAction.getFoundFailureCauses();
        removeNullCauses(foundFailureCauses);
        boolean hasFailureCauseFromBFA = false;
        for (FoundFailureCause cause : foundFailureCauses) {
            if (cause.getIndications().isEmpty()) {
                continue;
            }
            if (cause.getName().equals(newClaimedFailureCause.getName())
                    && cause.getIndications().get(0).getMatchingFile().equals("log")) {
                hasFailureCauseFromBFA = true;
            }
            // check if it's an indication created by claim
            if (cause.getIndications().get(0).getMatchingFile().equals(MATCHING_FILE)) {
                existingClaimedFoundFailureCause = cause;
                break;
            }
        }
        if (existingClaimedFoundFailureCause != null) {
            foundFailureCauses.remove(existingClaimedFoundFailureCause);
        }
        if (!hasFailureCauseFromBFA) {
            foundFailureCauses.add(newClaimedFailureCause);
        }
        try {
            run.save();
            StatisticsLogger.getInstance().log(run, bfaAction.getFoundFailureCauses());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeFailAction(Run run) {
        List<FailureCauseBuildAction> bfaActionList = run.getActions(FailureCauseBuildAction.class);
        if (!bfaActionList.isEmpty()) {
            FailureCauseBuildAction bfaAction = bfaActionList.get(0);
            List<FoundFailureCause> foundFailureCauses = bfaAction.getFoundFailureCauses();
            removeNullCauses(foundFailureCauses);
            List<FoundFailureCause> toRemove = Lists.newArrayList();
            for (FoundFailureCause cause : foundFailureCauses) {
                if (!cause.getIndications().isEmpty()
                        && cause.getIndications().get(0).getMatchingFile().equals(MATCHING_FILE)) {
                    toRemove.add(cause);
                }
            }
            foundFailureCauses.removeAll(toRemove);
        }
    }

    /**
     * Drops {@code null} entries from the found failure causes, which a build can end up with if its
     * {@code build.xml} is not fully deserialized. Removing them here, rather than skipping them in each loop,
     * also keeps them away from BFA's statistics logger, which does not tolerate them, and stops them being
     * saved back to the build.
     */
    private static void removeNullCauses(List<FoundFailureCause> foundFailureCauses) {
        foundFailureCauses.removeIf(Objects::isNull);
    }

}
