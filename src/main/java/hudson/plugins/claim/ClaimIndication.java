package hudson.plugins.claim;

import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.model.Run;

public class ClaimIndication extends FoundIndication {

    public ClaimIndication(Run run, String originalPattern, String matchingFile, String matchingString) {
        super(run, originalPattern, matchingFile, matchingString);
    }
}
