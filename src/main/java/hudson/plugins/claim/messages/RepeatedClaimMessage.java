package hudson.plugins.claim.messages;

import hudson.plugins.claim.Messages;

import java.util.Collections;

abstract class RepeatedClaimMessage extends ClaimMessage {

    RepeatedClaimMessage(String item, String url, String claimedByUser) {
        super(item, url, claimedByUser);
    }

    protected abstract String getSpecificMessage();

    @Override
    protected String getMessage() {
        return getSpecificMessage()
            + LINE_SEPARATOR
            + LINE_SEPARATOR
            + Messages.ClaimEmailer_Details(buildJenkinsUrl());
    }

    @Override
    protected Iterable<? extends String> getToRecipients() {
        return Collections.singleton(getClaimedByUser());
    }
}
