package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.User;
import hudson.tasks.junit.TestAction;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


public abstract class DescribableTestAction extends TestAction implements Describable<DescribableTestAction> {

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public final Descriptor<DescribableTestAction> getDescriptor() {
         return DESCRIPTOR;
    }

    private static Comparator<User> idComparator = Comparator.comparing(User::getId);
    private static Comparator<User> fullNameComparator = Comparator.comparing(User::getFullName)
                                                                   .thenComparing(idComparator);

    @Extension
    public static final class DescriptorImpl extends Descriptor<DescribableTestAction> {
        @Override
        public String getDisplayName() {
            return "Assignee";
        }

        public ListBoxModel doFillAssigneeItems() {
            ListBoxModel items = new ListBoxModel();

            // sort in case the users are not already in sort order
            // with the current user at the top of the list
            String currentUserId = Hudson.getAuthentication().getName();
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = User.get(currentUserId, false, Collections.emptyMap());
            }
            if (currentUser != null) {
                items.add(currentUser.getDisplayName(), currentUser.getId());
            }
            Collection<User> c = User.getAll();
            if (currentUser != null) {
                if (c.contains(currentUser)) {
                    c.remove(currentUser);
                }
            }

            List<User> l = new ArrayList<>(c);
            l.sort(getComparator());
            for (User u : l) {
                items.add(u.getDisplayName(), u.getId());
            }

            return items;
        }

        public ListBoxModel doFillErrorsItems(@AncestorInPath Run run) throws Exception {

            ListBoxModel items = new ListBoxModel();
            if (ClaimBuildFailureAnalyzer.isBFAEnabled()) {
                LinkedList<String> list = ClaimBuildFailureAnalyzer.getDropdownList();
                AbstractClaimBuildAction action = run.getAction(AbstractClaimBuildAction.class);
                if (action == null || action.getBfaClaimer() == null || !action.isClaimed()) {
                    items.add("---None---", ClaimBuildFailureAnalyzer.DEFAULT_ERROR);
                    for (String cause : list) {
                        items.add(cause, cause);
                    }
                } else {
                    ClaimBuildFailureAnalyzer bfaClaimer = action.getBfaClaimer();
                    if (!bfaClaimer.isDefaultError()) {
                        items.add(bfaClaimer.getError(), bfaClaimer.getError());
                    }
                    items.add("---None---", ClaimBuildFailureAnalyzer.DEFAULT_ERROR);
                    for (String cause : list) {
                        if (!cause.equals(bfaClaimer.getError())) {
                            items.add(cause, cause);
                        }
                    }
                }
            }
            return items;
        }
    }

    private static Comparator<? super User> getComparator() {
        if (ClaimConfig.get().isSortUsersByFullName()) {
            return fullNameComparator;
        } else {
            return idComparator;
        }
    }
}
