package hudson.plugins.claim;

public class ClaimColumnInformation {

    private boolean matrix;
    private String combinationName;
    private ClaimBuildAction claim;

    public ClaimBuildAction getClaim() {
        return claim;
    }

    public void setClaim(ClaimBuildAction claim) {
        this.claim = claim;
    }

    public String getCombinationName() {
        return combinationName;
    }

    public void setCombinationName(String combinationName) {
        this.combinationName = combinationName;
    }

    public boolean isMatrix() {
        return matrix;
    }

    public void setMatrix(boolean matrix) {
        this.matrix = matrix;
    }
}
