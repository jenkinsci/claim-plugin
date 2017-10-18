package hudson.plugins.claim;

import com.google.common.collect.Lists;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import com.sonyericsson.jenkins.plugins.bfa.statistics.StatisticsLogger;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ClaimBuildFailureAnalyzer {

    public static String ERROR = "Default";
    private static final String matchingFile = "Claim";

    public ClaimBuildFailureAnalyzer(String error) throws Exception {
        ERROR=error;
    }

    public static Collection<FailureCause> getFailureCauses() throws Exception {
        return Jenkins.getInstance().getPlugin(PluginImpl.class).getKnowledgeBase().getCauses();
    }

    public static boolean isBFAEnabled(){
        return (Jenkins.getInstance().getPlugin("build-failure-analyzer")!=null && Jenkins.getInstance().getPlugin(PluginImpl.class).isGlobalEnabled());
    }

    public static HashMap<String, String> getFillReasonMap() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        for (FailureCause cause : getFailureCauses()) {
            map.put(cause.getName(), cause.getDescription());
        }
        return map;
    }

    public static LinkedList<String> getDropdownList() throws Exception {
        LinkedList<String> list = new LinkedList<String>();
        for (FailureCause cause : getFailureCauses()) {
            list.add(cause.getName());
        }
        return list;
    }

    public void createFailAction(Run run) throws Exception {
        FoundFailureCause newClaimedFailureCause = null;
        List<FoundIndication> indications = new LinkedList<FoundIndication>();
        for(FailureCause cause : getFailureCauses()){
            if(cause.getName().equals(ERROR)) {
                indications.add(new ClaimIndication( run,"Null",matchingFile,"Null"));
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
            if(cause.getName().equals(newClaimedFailureCause.getName()) && cause.getIndications().get(0).getMatchingFile().equals("log")){
                hasFailureCauseFromBFA = true;
            }
            if (cause.getIndications().get(0).getMatchingFile()==matchingFile) {
                existingClaimedFoundFailureCause = cause;
                break;
            }
        }
        if (existingClaimedFoundFailureCause != null) {
            foundFailureCauses.remove(existingClaimedFoundFailureCause);
        }
        if(!hasFailureCauseFromBFA) {
            foundFailureCauses.add(newClaimedFailureCause);
        }
        try {
            run.save();
            StatisticsLogger.getInstance().log((AbstractBuild) run, bfaAction.getFoundFailureCauses());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeFailAction(Run run){
        List<FailureCauseBuildAction> bfaActionList = run.getActions(FailureCauseBuildAction.class);
        if(!bfaActionList.isEmpty()) {
            FailureCauseBuildAction bfaAction = bfaActionList.get(0);
            List<FoundFailureCause> foundFailureCauses = bfaAction.getFoundFailureCauses();
            List<FoundFailureCause> toRemove = Lists.newArrayList();
            for (FoundFailureCause cause : foundFailureCauses) {
                if (cause.getIndications().get(0).getMatchingFile() == "Claim") {
                    toRemove.add(cause);
                }
            }
            for (FoundFailureCause cause : toRemove) {
                foundFailureCauses.remove(cause);
            }
        }
    }

}
