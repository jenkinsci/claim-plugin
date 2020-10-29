package hudson.plugins.claim;

import hudson.tasks.test.AbstractTestResultAction;

public final class UnclaimedTestfailuresColumnInformation {

	private int nbClaimedFailures;
	private AbstractTestResultAction<?> testResultAction;

	public UnclaimedTestfailuresColumnInformation(AbstractTestResultAction<?> testResultAction, int nbClaimedFailures) {
		this.testResultAction = testResultAction;
		this.nbClaimedFailures = nbClaimedFailures;
	}

	public int getNbClaimedFailures() {
		return nbClaimedFailures;
	}

	public int getNbUnclaimedFailures() {
		return testResultAction.getFailCount() - nbClaimedFailures;
	}

	public AbstractTestResultAction<?> getTestResultAction() {
		return testResultAction;
	}
}
