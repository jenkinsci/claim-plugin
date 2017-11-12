package hudson.plugins.claim;

import hudson.model.Run;

public class ClaimBuildAction extends AbstractClaimBuildAction<Run> {

    private static final long serialVersionUID = 1L;

    private transient Run run;

    public ClaimBuildAction(Run run) {
        super(run);
    }

    public String getDisplayName() {
        return Messages.ClaimBuildAction_DisplayName();
    }

    @Override
    public String getNoun() {
        return Messages.ClaimBuildAction_Noun();
    }

    public Object readResolve() {
        if (run != null && owner == null) {
            owner = run;
        }
        return this;
    }

    @Override
    String getUrl() {
        return owner.getUrl();
    }

}
