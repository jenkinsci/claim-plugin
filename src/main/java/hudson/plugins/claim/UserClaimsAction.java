package hudson.plugins.claim;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.ModelObject;
import hudson.model.TransientUserActionFactory;
import hudson.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class UserClaimsAction extends AbstractAssignedClaimsReport {

    /**
     * Add the {@link UserClaimsAction} to all {@link User} instances.
     */
    @Extension(ordinal = -1000)
    public static class TransientUserClaimsActionactoryImpl extends TransientUserActionFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<? extends Action> createFor(User target) {
            return Collections.singleton(new UserClaimsAction(target));
        }
    }
    
    @NonNull
    private final User targetUser;

    public UserClaimsAction(@NonNull User user) {
        targetUser = user;
    }
    
    @Override
    public ModelObject getOwner() {
        return targetUser;
    }

    @Override
    protected boolean isDisplayed(AbstractClaimBuildAction<?> claimAction) {
        return super.isDisplayed(claimAction)
            && User.get(claimAction.getClaimedBy(), false, Collections.emptyMap()).getId()
                .equals(targetUser.getId());
    }


    @Override
    public String getDisplayName() {
        User currentUser = User.current();
        if (currentUser != null && Objects.equals(targetUser.getId(), currentUser.getId())) {
            return Messages.UserClaimsAction_DisplayName_MyClaims();
        }
        return Messages.UserClaimsAction_DisplayName_AssignedClaims();
    }

    @Override
    public String getUrlName() {
        return "claims";
    }
}
