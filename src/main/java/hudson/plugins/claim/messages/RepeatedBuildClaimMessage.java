package hudson.plugins.claim.messages;

import hudson.plugins.claim.Messages;

public class RepeatedBuildClaimMessage extends RepeatedClaimMessage {

    public RepeatedBuildClaimMessage(String item, String url, String claimedByUser) {
        super(item, url, claimedByUser);
    }

    @Override
    protected boolean mustBeSent() {
        return true;
    }

    @Override
    protected String getSubject() {
        return Messages.ClaimEmailer_Build_Repeated_Subject(getItem());
    }

    @Override
    protected String getSpecificMessage() {
        return "";
    }
}
