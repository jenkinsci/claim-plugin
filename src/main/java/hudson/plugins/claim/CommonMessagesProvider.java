package hudson.plugins.claim;

import hudson.model.User;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.function.Supplier;

import static hudson.security.ACL.ANONYMOUS_USERNAME;

public final class CommonMessagesProvider {

    private static final int DATA_PRESENT = 1;
    private static final int DATA_ABSENT = 0;
    private static final String NO_DATA = "";
    private static final String TERMINATOR = NO_DATA;

    /**
     * Provides an instance of {@link CommonMessagesProvider} ready to provide messages based on the build action.
     * @param action the action to use to get claim information
     * @return an instance of {@link CommonMessagesProvider}
     */
    @Nonnull
    static CommonMessagesProvider build(@Nonnull AbstractClaimBuildAction action) {
        return build(action.isClaimed(), action.getClaimedBy(), action.getAssignedBy(), action.getClaimDate());
    }

    /**
     * Provides an instance of {@link CommonMessagesProvider} ready to provide messages based on provided data.
     *
     * @param claimed true if the object was claimed
     * @param claimedBy of the claiming user
     * @param assignedBy of the assigned user
     * @param date date of the claim
     * @return an instance of {@link CommonMessagesProvider}
     */
    @Nonnull
    private static CommonMessagesProvider build(boolean claimed, String claimedBy, String assignedBy, Date date) {
        return new CommonMessagesProvider(claimed, claimedBy, assignedBy, date);
    }

    private final boolean claimed;
    private final String claimedBy;
    private final String assignedBy;
    private final Date date;
    private final Supplier<Formatter> objectFormatSupplier;
    private final Supplier<Formatter> formatSupplier;

    private CommonMessagesProvider(boolean claimed, String claimedBy, String assignedBy, Date date) {
        this.claimed = claimed;
        this.claimedBy = claimedBy;
        this.assignedBy = assignedBy;
        this.date = date;
        this.formatSupplier = () -> getFormat(new MessagesProvider() {
            @Override
            public Formatter notClaimed() {
                return Messages::CommonMessages_NoObject_Unclaim;
            }
            @Override
            public Formatter claimedBySelf() {
                return Messages::CommonMessages_NoObject_Claim_Self;
            }
            @Override
            public Formatter assignedBySelf() {
                return Messages::CommonMessages_NoObject_Assign_Self;
            }
            @Override
            public Formatter assignedToSelf() {
                return Messages::CommonMessages_NoObject_Assign_ToSelf;
            }
            @Override
            public Formatter claimedByOther() {
                return Messages::CommonMessages_NoObject_Claim_Other;
            }
            @Override
            public Formatter assignedByAndToOther() {
                return Messages::CommonMessages_NoObject_Assign_Other;
            }
        });
        this.objectFormatSupplier = () -> getFormat(new MessagesProvider() {
            @Override
            public Formatter notClaimed() {
                return Messages::CommonMessages_Object_Unclaim;
            }
            @Override
            public Formatter claimedBySelf() {
                return Messages::CommonMessages_Object_Claim_Self;
            }
            @Override
            public Formatter assignedBySelf() {
                return Messages::CommonMessages_Object_Assign_Self;
            }
            @Override
            public Formatter assignedToSelf() {
                return Messages::CommonMessages_Object_Assign_ToSelf;
            }
            @Override
            public Formatter claimedByOther() {
                return Messages::CommonMessages_Object_Claim_Other;
            }
            @Override
            public Formatter assignedByAndToOther() {
                return Messages::CommonMessages_Object_Assign_Other;
            }
        });
    }

    private Formatter getFormat(MessagesProvider messagesProvider) {
        if (!claimed) {
          return messagesProvider.notClaimed();
        }
        Authentication auth = Jenkins.getAuthentication();
        String currentUser = auth.getName();
        boolean isAutoAssigned = claimedBy.equals(assignedBy);
        if (!currentUser.equals(ANONYMOUS_USERNAME)) {
            if (currentUser.equals(assignedBy)) {
                if (isAutoAssigned) {
                    return messagesProvider.claimedBySelf();
                } else {
                    return messagesProvider.assignedBySelf();
                }
            } else {
                if (currentUser.equals(claimedBy)) {
                    return messagesProvider.assignedToSelf();
                }
            }
        }
        if (isAutoAssigned) {
            return messagesProvider.claimedByOther();
        }
        return messagesProvider.assignedByAndToOther();
    }

    public String getFullClaimDescription(String objectName) {
        return formatClaimDescription(objectFormatSupplier.get(),
                objectName,
                assignedBy,
                claimedBy,
                isDataPresent(date),
                date,
                true);
    }

    public String getFullClaimDescription() {
        return formatClaimDescription(formatSupplier.get(),
                NO_DATA,
                assignedBy,
                claimedBy,
                isDataPresent(date),
                date,
                true);
    }

    public String getShortClaimDescription(String objectName) {
        return formatClaimDescription(objectFormatSupplier.get(),
                objectName,
                assignedBy,
                claimedBy,
                DATA_ABSENT,
                date,
                false);
    }

    public String getShortClaimDescription() {
        return formatClaimDescription(formatSupplier.get(),
                NO_DATA,
                assignedBy,
                claimedBy,
                DATA_ABSENT,
                date,
                false);
    }

    public String getReasonTitle() {
        return Messages.CommonMessages_Reason();
    }

    private static String formatClaimDescription(Formatter formatter, String objectName, String assignedBy,
                                                 String claimedBy, int hasDate, Date date, boolean enhanceUserLinks) {
        return formatter.format(
                objectName,
                getUserInfo(assignedBy, enhanceUserLinks),
                getUserInfo(claimedBy, enhanceUserLinks),
                hasDate,
                date,
                TERMINATOR);
    }

    private static String getUserInfo(String userName, boolean enhanceUserLinks) {
        User user = User.get(userName, false, Collections.emptyMap());
        String displayName = ACL.ANONYMOUS_USERNAME;
        if (user != null) {
            displayName = user.getDisplayName();
            if (StringUtils.isEmpty(displayName)) {
                displayName = userName;
            }
        }
        if (enhanceUserLinks && user != null) {
            return "<a href=\"" + user.getAbsoluteUrl() + "\">" + displayName + "</a>";
        }
        return displayName;
    }

    private static int isDataPresent(Date data) {
        if (data == null) {
            return DATA_ABSENT;
        }
        return DATA_PRESENT;
    }

    private interface MessagesProvider {
        Formatter notClaimed();
        Formatter claimedBySelf();
        Formatter assignedBySelf();
        Formatter assignedToSelf();
        Formatter claimedByOther();
        Formatter assignedByAndToOther();
    }

    @FunctionalInterface
    private interface Formatter {
        String format(Object objectName, Object assignedBy, Object claimedBy, Object hasDate, Object date, Object
                notUsed);
    }
}
