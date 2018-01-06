package hudson.plugins.claim.utils;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

public class TestBuilder extends Builder {

    private Result result = Result.FAILURE;

    /**
     * Sets a result to be returned by this publisher.
     * @param result the result
     */
    public void setResult(Result result) {
        this.result = result;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        listener.getLogger().println("Simulating a specific result code " + result);
        build.setResult(result);
        return true;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return new TestBuilder.DescriptorImpl();
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public Builder newInstance(StaplerRequest req, JSONObject data) {
            throw new UnsupportedOperationException();
        }
    }
}
