/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.claim;

/**
 *
 * @author henrik
 */
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
