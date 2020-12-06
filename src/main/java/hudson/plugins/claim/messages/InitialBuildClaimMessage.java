package hudson.plugins.claim.messages;

import hudson.plugins.claim.Messages;

public final class InitialBuildClaimMessage extends InitialClaimMessage {
    public InitialBuildClaimMessage(String item, String url, String reason, String claimedByUser,
                                    String assignedByUser) {
        super(item, url, reason, claimedByUser, assignedByUser);
    }

    @Override
    protected String getText(String action, String assignedByUser) {
        return Messages.ClaimEmailer_Build_Initial_Text(action, assignedByUser);
    }

    @Override
    protected String getSubject(String action) {
        return Messages.ClaimEmailer_Build_Initial_Subject(action);
    }
}

