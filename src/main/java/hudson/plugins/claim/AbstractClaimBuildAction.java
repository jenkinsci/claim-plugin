package hudson.plugins.claim;

import hudson.model.BuildBadgeAction;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Item;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.tasks.junit.TestAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 2)
public abstract class AbstractClaimBuildAction<T extends Saveable> extends TestAction implements BuildBadgeAction,
		ProminentProjectAction {

	private static final long serialVersionUID = 1L;
	private final String INVALID_ENTRY = "Not Selected";
	private final String DELIMITER = ":";

	private boolean claimed;
	private String claimedBy;
	private boolean transientClaim;
	private Map<String, Map<String, AbstractBuild>> culpritBuildMap;
	private List<AbstractBuild> selectedBuildList;

	protected T owner;

	AbstractClaimBuildAction(T owner) {
		this.owner = owner;
	}

	private Map<String, AbstractBuild> getBuildsOfSameCulprit(User culprit) {
		Map<String, AbstractBuild> buildList = new HashMap<String, AbstractBuild>();

		for(AbstractBuild build : getAllUnstableBuilds()) {
			if(isCulpritOfBuild(build, culprit)) {
				buildList.put(createMapId(culprit.getId(), build.getFullDisplayName()), build);
			}
		}

		return buildList;
	}

	private String createMapId(String culprit, String buildFullDisplayName) {
		return culprit.hashCode() + DELIMITER + buildFullDisplayName.hashCode();
	}

	private boolean isCulpritOfBuild(AbstractBuild build, User culprit) {

		if(build.getCulprits().contains(culprit)) {
			return true;
		}

		return false;
	}

	private boolean isBuildUnstable(AbstractBuild build) {
		if(build == null) {
			return false;
		}

		Result result = build.getResult();

		if(result == null) {
			return false;
		}

		return result.isWorseThan(Result.SUCCESS);
	}

	private List<AbstractBuild> getAllUnstableBuilds() {
		List<? extends Item> items = this.getItems();

		List<AbstractBuild> buildList = new ArrayList<AbstractBuild>();

		if(items == null) {
			return buildList;
		}

		/*loop over the jobs*/
		for(Item item : items) {
			Job<?, ?> job = (Job<?, ?>) item;

			AbstractBuild<?, ?> lastBuild = (AbstractBuild<?, ?>)job.getLastBuild();

			if(isBuildUnstable(lastBuild)) {
				buildList.add(lastBuild);
			}
		}

		return buildList;
	}

	@Exported
	public Set<User> getCulprits() {
		AbstractBuild<?, ?> currentBuild = (AbstractBuild<?, ?>) owner;

		if(currentBuild == null) {
			return new HashSet<User>();
		}

		return currentBuild.getCulprits();
	}

	@Exported
	public Map<String, Map<String, AbstractBuild>> getCulpritBuildMap() {
		culpritBuildMap = new HashMap<String, Map<String,AbstractBuild>>();

		for(User culprit : this.getCulprits()) {
			culpritBuildMap.put(culprit.getId(), getBuildsOfSameCulprit(culprit));
		}

		return culpritBuildMap;
	}

	public List<? extends Item> getItems() {
		return Hudson.getInstance().getItems();
	}

	private String reason;

	public String getIconFileName() {
		return null;
	}

	public String getUrlName() {
		return "claim";
	}

	public void doClaim(StaplerRequest req, StaplerResponse resp)
			throws ServletException, IOException {

		Authentication authentication = Hudson.getAuthentication();
		String name = authentication.getName();
		String reason = (String) req.getSubmittedForm().get("reason");
		String culprit = (String) req.getSubmittedForm().get("culprits");
		boolean sticky = req.getSubmittedForm().getBoolean("sticky");

		if(!StringUtils.equals(culprit, INVALID_ENTRY)) {
			selectedBuildList = getSelectedBuilds(req, culprit);
			claimSelectedBuilds(name, reason, sticky, culprit);
		}

		if (StringUtils.isEmpty(reason)) reason = null;

		claim(name, reason, sticky);
		owner.save();

		resp.forwardToPreviousPage(req);
	}

	private void claimSelectedBuilds(String name, String reason, boolean sticky, String culprit) throws IOException {
		for(AbstractBuild selectedBuild : selectedBuildList) {
			claimGivenBuild(name, reason, sticky, culprit, selectedBuild);
		}
	}

	private List<AbstractBuild> getSelectedBuilds(StaplerRequest req, String culprit) throws ServletException {
		List<AbstractBuild> selectedBuildList = new ArrayList<AbstractBuild>();

		Map<String, AbstractBuild> buildMap = culpritBuildMap.get(culprit);

		for(String buildId : buildMap.keySet()) {
			if(!isSelected(req, buildId)) {
				continue;
			}

			selectedBuildList.add(buildMap.get(buildId));
		}

		return selectedBuildList;
	}

	private boolean isSelected(StaplerRequest req, String buildId) throws ServletException {
		return req.getSubmittedForm().getBoolean(buildId);
	}

	public void claimGivenBuild(String name, String reason, boolean sticky, String culprit, AbstractBuild<?, ?> otherBuild) throws IOException {
		ClaimBuildAction otherClaim = otherBuild.getAction(ClaimBuildAction.class);

		if(otherClaim == null) {
			return;
		}

		if(!otherClaim.isClaimed() && otherClaim.isSameCulprit(culprit)) {
			otherClaim.claim(name, reason, sticky);
			otherBuild.save();
		}
	}

	public boolean isSameCulprit(String culprit) {

		Set<User> culprits = this.getCulprits();

		if(culprit == null || culprit.isEmpty() || culprits.isEmpty()) {
			return false;
		}

		for(User user : culprits) {
			if(StringUtils.equalsIgnoreCase(user.getId(), culprit)){
				return true;
			}
		}

		return false;
	}

	public void doUnclaim(StaplerRequest req, StaplerResponse resp)
			throws ServletException, IOException {
		unclaim();
		owner.save();
		resp.forwardToPreviousPage(req);
	}

	@Exported
	public String getClaimedBy() {
		return claimedBy;
	}

	public String getClaimedByName() {
		User user = User.get(claimedBy, false);
		if (user != null) {
			return user.getDisplayName();
		} else {
			return claimedBy;
		}
	}

	public void setClaimedBy(String claimedBy) {
		this.claimedBy = claimedBy;
	}

	@Exported
	public boolean isClaimed() {
		return claimed;
	}

	public void claim(String claimedBy, String reason, boolean sticky) {
		this.claimed = true;
		this.claimedBy = claimedBy;
		this.reason = reason;
		this.transientClaim = !sticky;
	}

	/**
	 * Claim a new Run with the same settings as this one.
	 */
	public void copyTo(AbstractClaimBuildAction other) {
		other.claim(getClaimedBy(), getReason(), isSticky());
	}

	public void unclaim() {
		this.claimed = false;
		this.claimedBy = null;
		this.transientClaim = false;
		// we remember the reason to show it if someone reclaims this build.
	}

	public boolean isClaimedByMe() {
		return !isUserAnonymous()
				&& Hudson.getAuthentication().getName().equals(claimedBy);
	}

	public boolean canClaim() {
		return !isUserAnonymous() && !isClaimedByMe();
	}

	public boolean canRelease() {
		return !isUserAnonymous() && isClaimedByMe();
	}

	public boolean isUserAnonymous() {
		return Hudson.getAuthentication().getName().equals("anonymous");
	}

	@Exported
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean hasReason() {
		return !StringUtils.isEmpty(reason);
	}

	public boolean isTransient() {
		return transientClaim;
	}

	public void setTransient(boolean transientClaim) {
		this.transientClaim = transientClaim;
	}

	public boolean isSticky() {
		return !transientClaim;
	}

	public void setSticky(boolean sticky) {
		this.transientClaim = !sticky;
	}

	public abstract String getNoun();

}