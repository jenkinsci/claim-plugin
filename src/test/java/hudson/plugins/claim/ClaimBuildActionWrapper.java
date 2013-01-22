package hudson.plugins.claim;

import java.util.List;
import java.util.Set;

import hudson.model.Run;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.User;

public class ClaimBuildActionWrapper extends ClaimBuildAction {

	private Set<User> culprits;
	private List<? extends Item> items;

	ClaimBuildActionWrapper(Run owner) {
		super(owner);
	}

	@Override
	public Set<User> getCulprits() {
		if(culprits == null) {
			return super.getCulprits();
		}

		return culprits;
	}

	public void setCulprits(Set<User> culprits) {
		this.culprits = culprits;
	}

	@Override
	public List<? extends Item> getItems() {
		if(items == null) {
			return super.getItems();
		}

		return items;
	}

	public void setItems(List<? extends Item> items) {
		this.items = items;
	}

}
