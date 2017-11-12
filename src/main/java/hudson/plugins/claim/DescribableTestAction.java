package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.tasks.junit.TestAction;
import hudson.util.ListBoxModel;

import java.util.*;

public abstract class DescribableTestAction extends TestAction implements Describable<DescribableTestAction> {

	//private static final Logger LOGGER = Logger.getLogger("claim-plugin");

	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public Descriptor<DescribableTestAction> getDescriptor() {
		return DESCRIPTOR;
	}
	
	private static Comparator<User> idComparator = Comparator.comparing(User::getId);
	private static Comparator<User> fullNameComparator = Comparator.comparing(User::getFullName)
			                                                           .thenComparing(idComparator);

	@Extension
	public static final class DescriptorImpl extends Descriptor<DescribableTestAction> {
		@Override
		public String getDisplayName() { return "Assignee"; }

		public ListBoxModel doFillAssigneeItems() {
			ListBoxModel items = new ListBoxModel();

			// sort in case the users are not already in sort order
			// with the current user at the top of the list
			String currentUserId = Hudson.getAuthentication().getName();
			User currentUser = null;
			if (currentUserId != null) {
				currentUser = User.get(currentUserId);
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

		public ListBoxModel doFillErrorsItems() throws Exception {
			ListBoxModel items = new ListBoxModel();
			if (ClaimBuildFailureAnalyzer.isBFAEnabled()) {
				LinkedList<String> list = ClaimBuildFailureAnalyzer.getDropdownList();
				if (!AbstractClaimBuildAction.isReclaim) {
					items.add("---None---", "Default");
					for (String cause : list) {
						items.add(cause, cause);
					}
				} else {
					if (!ClaimBuildFailureAnalyzer.ERROR.equals("Default")) {
						items.add(ClaimBuildFailureAnalyzer.ERROR, ClaimBuildFailureAnalyzer.ERROR);
					}
					items.add("---None---", "Default");
					for (String cause : list) {
						if (!cause.equals(ClaimBuildFailureAnalyzer.ERROR))
							items.add(cause, cause);
					}
				}
			}
			return items;
		}
	}

	private static Comparator<? super User> getComparator() {
		return ClaimConfig.get().isSortUsersByFullName()
				? fullNameComparator
				: idComparator;
	}
}
