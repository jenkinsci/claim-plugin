package hudson.plugins.claim;

import java.io.ObjectStreamException;

import hudson.model.Run;

public class ClaimBuildAction extends AbstractClaimBuildAction<Run> {

	private static final long serialVersionUID = 1L;
	
	private transient Run run;

	public ClaimBuildAction(Run run) {
		super(run);
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

}
