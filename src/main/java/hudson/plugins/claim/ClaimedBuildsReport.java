package hudson.plugins.claim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.Action;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.model.ViewDescriptor;
import hudson.util.RunList;

public class ClaimedBuildsReport extends View implements Action {
    private final Hudson owner;

    public ClaimedBuildsReport(Hudson owner) {
        super("Claim Report");
    	this.owner = owner;
    }
    
    public Hudson getParent() {
    	return this.owner;
    }
    
	public String getIconFileName() {
		return "orange-square.gif";
	}

	public String getUrlName() {
		return "claims";
	}

	@Override
	public boolean contains(TopLevelItem item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Item doCreateItem(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TopLevelItem getItem(String name) {
		return owner.getItem(name);
	}

	@Override
	public Collection<TopLevelItem> getItems() {
		return owner.getItems();
	}

	@Override
	public String getUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onJobRenamed(Item item, String oldName, String newName) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void submit(StaplerRequest req) throws IOException, ServletException, FormException {
		// TODO Auto-generated method stub
	}

	@Override
	public ViewDescriptor getDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	public Run getFirstFail(Run r) {
		Run lastGood = r.getPreviousNotFailedBuild();
		Run firstFail;
		if(lastGood == null) {
			firstFail = r.getParent().getFirstBuild();
		} else {
			firstFail = lastGood.getNextBuild();
		}
		return firstFail;
	}
	
	public String getClaimantText(Run r) {
		ClaimBuildAction claim = r.getAction(ClaimBuildAction.class);
		if(claim == null || !claim.isClaimed()) {
			return "unclaimed";
		}
		String reason = claim.getReason();
		if (reason != null) {
			return "claimed by " + claim.getClaimedBy() + " because: " + claim.getReason();
		} else {
			return "claimed by " + claim.getClaimedBy();
		}
	}
	
	@Override
	public RunList getBuilds() {
        List<Run> lastBuilds = new ArrayList<Run>();
        for (TopLevelItem item : getItems()) {
            if (item instanceof Job) {
                Job job = (Job) item;
                Run lb = job.getLastBuild();
                while (lb != null && (lb.hasntStartedYet() || lb.isBuilding()))
                    lb = lb.getPreviousBuild();

                if(lb!=null && lb.getAction(ClaimBuildAction.class) != null)  {
                	lastBuilds.add(lb);
                }
            }
        }

        return RunList.fromRuns(lastBuilds).failureOnly();
	}

	
}
