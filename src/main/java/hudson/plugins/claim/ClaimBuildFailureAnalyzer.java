package hudson.plugins.claim;

import com.google.common.collect.Lists;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.StatisticsLogger;
import hudson.model.Run;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public final class ClaimBuildFailureAnalyzer {

    public static final String DEFAULT_ERROR = "Default";
    private static final String MATCHING_FILE = "Claim";

    @Nonnull
    private final String error;

    public ClaimBuildFailureAnalyzer(@Nonnull String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public boolean isDefaultError() {
        return DEFAULT_ERROR.equals(error);
    }

    public static Collection<FailureCause> getFailureCauses() throws Exception {
        return Jenkins.getInstance().getPlugin(PluginImpl.class).getKnowledgeBase().getCauses();
    }

    public static boolean isBFAEnabled() {
        return (Jenkins.getInstance().getPlugin("build-failure-analyzer") != null
                && Jenkins.getInstance().getPlugin(PluginImpl.class).isGlobalEnabled());
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
        try {
            Jenkins.getInstance().getPlugin(PluginImpl.class).save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<FailureCauseBuildAction> bfaActionList = run.getActions(FailureCauseBuildAction.class);
        FoundFailureCause existingClaimedFoundFailureCause = null;
        FailureCauseBuildAction bfaAction = bfaActionList.get(0);
        List<FoundFailureCause> foundFailureCauses = bfaAction.getFoundFailureCauses();
        boolean hasFailureCauseFromBFA = false;
        for (FoundFailureCause cause : foundFailureCauses) {
            // check if it's an indication created by claim
            if (cause.getName().equals(newClaimedFailureCause.getName())
                    && cause.getIndications().get(0).getMatchingFile().equals("log")) {
                hasFailureCauseFromBFA = true;
            }
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
            List<FoundFailureCause> toRemove = Lists.newArrayList();
            for (FoundFailureCause cause : foundFailureCauses) {
                if (cause.getIndications().size() > 0
                        && cause.getIndications().get(0).getMatchingFile().equals(MATCHING_FILE)) {
                    toRemove.add(cause);
                }
            }
            foundFailureCauses.removeAll(toRemove);
        }
    }

}
