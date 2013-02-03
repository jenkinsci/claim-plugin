package hudson.plugins.claim;

import java.io.ObjectStreamException;

import hudson.model.AbstractBuild;
import hudson.model.Api;
import hudson.model.Run;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility=2)
public class ClaimBuildAction extends AbstractClaimBuildAction<AbstractBuild> {

	private static final long serialVersionUID = 1L;
	
	private transient AbstractBuild run;

	public ClaimBuildAction(AbstractBuild run) {
		super(run);
	}

    @Override
    public AbstractBuild<?, ?> getBuild() {
        return run;
    }

    public String getDisplayName() {
		return "Claim Build";
	}
	
	@Override
	public String getNoun() {
		return "build";
	}

	public Object readResolve() throws ObjectStreamException {
		if (run != null && owner == null) {
			owner = run;
		}
		return this;
	}

    public Api getApi() {
        return new Api(new ClaimedBuildsReport.ClaimReportEntry(owner));
    }

}
