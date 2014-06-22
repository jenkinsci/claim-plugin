package hudson.plugins.claim;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.tasks.junit.TestAction;
import hudson.util.ListBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class DescribableTestAction extends TestAction implements Describable<DescribableTestAction> {

	//private static final Logger LOGGER = Logger.getLogger("claim-plugin");

	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public Descriptor<DescribableTestAction> getDescriptor() {
		return DESCRIPTOR;
	}
	
	private static Comparator<User> comparator = new Comparator<User>() {
		public int compare(User c1, User c2) {
			return c1.getId().compareTo(c2.getId()); 
		}
	};

	@Extension
	public static final class DescriptorImpl extends Descriptor<DescribableTestAction> {
		@Override
		public String getDisplayName() { return "Assignee"; }

		public ListBoxModel doFillAssigneeItems() {
			ListBoxModel items = new ListBoxModel();

			// sort in case the users are not already in sort order
			Collection<User> c = User.getAll();
			List<User> l = new ArrayList<User>(c);

			Collections.sort(l, comparator); 
			for (User u : l) {
				items.add(u.getDisplayName(), u.getId());
			}

			return items;

		}
	}

}
