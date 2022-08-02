package hudson.plugins.claim;

import hudson.model.Run;
import jenkins.model.RunAction2;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.Optional;

public final class ClaimBuildAction extends AbstractClaimBuildAction<Run> implements RunAction2 {

    private static final long serialVersionUID = 1L;

    private transient Run owner;

    public String getDisplayName() {
        return Messages.ClaimBuildAction_DisplayName();
    }

    @Override
    public String getNoun() {
        return Messages.ClaimBuildAction_Noun();
    }

    @Override
    String getUrl() {
        return owner.getUrl();
    }

    @Override
    protected Run getOwner() {
        return owner;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        owner = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        owner = run;
    }

    @Override
    protected Optional<AbstractClaimBuildAction> getNextAction() {
        Run nextRun = owner.getNextBuild();
        if (nextRun != null) {
            ClaimBuildAction action = nextRun.getAction(ClaimBuildAction.class);
            return Optional.ofNullable(action);
        }
        return Optional.empty();
    }

    @Override
    protected void sendInitialClaimEmail(String claimedByUser, String providedReason, String assignedByUser)
        throws MessagingException, IOException {
        ClaimEmailer.sendInitialBuildClaimEmailIfConfigured(
            claimedByUser,
            assignedByUser,
            getOwner().toString(),
            providedReason,
            getUrl());
    }
}
