package hudson.plugins.claim.messages;

import hudson.plugins.claim.Messages;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;

abstract class InitialClaimMessage extends ClaimMessage {
    private String reason;
    private String assignedByUser;

    protected InitialClaimMessage(String item, String url, String reason, String claimedByUser, String assignedByUser) {
        super(item, url, claimedByUser);
        this.reason = reason;
        this.assignedByUser = assignedByUser;
    }

    protected abstract String getText(String action, String user);

    protected abstract String getSubject(String action);

    @Override
    protected String getMessage() {
        return getText(getItem(), assignedByUser)
            + LINE_SEPARATOR
            + Messages.ClaimEmailer_Reason(reason)
            + LINE_SEPARATOR
            + LINE_SEPARATOR
            + Messages.ClaimEmailer_Details(buildJenkinsUrl());
    }

    @Override
    protected String getSubject() {
        return getSubject(getItem());
    }

    @Override
    protected Iterable<? extends String> getToRecipients() {
        return Collections.singleton(getClaimedByUser());
    }

    @Override
    protected boolean mustBeSent() {
        return !StringUtils.equals(getClaimedByUser(), assignedByUser);
    }
}
