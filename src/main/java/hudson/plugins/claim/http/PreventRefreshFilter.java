package hudson.plugins.claim.http;

import com.google.inject.Injector;
import hudson.Extension;
import hudson.XmlFile;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.plugins.claim.ClaimConfig;
import hudson.util.PluginServletFilter;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;

@Restricted(NoExternalUse.class)
@Extension
@SuppressWarnings("unused")
public class PreventRefreshFilter extends SaveableListener implements Filter  {

    private static final String NO_REFRESH_HEADER = PreventRefreshFilter.class.getName();

    public static void preventRefresh(StaplerResponse response) {
        response.addHeader(PreventRefreshFilter.NO_REFRESH_HEADER, "");
    }

    private static PreventRefreshFilter installedFilter;

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    @SuppressWarnings("WeakerAccess")
    public static synchronized  void initAutoRefreshFilter(ClaimConfig config) throws ServletException {
        boolean newInstalled = config.isBlockAutoRefreshWhileClaiming();
        boolean installed = installedFilter != null;
        if (newInstalled != installed) {
            Injector inj = Jenkins.getInstance().getInjector();
            if (newInstalled) {
                installedFilter = inj.getInstance(PreventRefreshFilter.class);
                PluginServletFilter.addFilter(installedFilter);
            } else {
                PluginServletFilter.removeFilter(installedFilter);
                installedFilter = null;
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ServletResponse actualResponse = response;
        if (response instanceof HttpServletResponse) {
            actualResponse = new AutoRefreshControlHttpResponse((HttpServletResponse) response, NO_REFRESH_HEADER);
        }
        chain.doFilter(request, actualResponse);
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void onChange(Saveable o, XmlFile file) {
        if (o instanceof ClaimConfig) {
            try {
                initAutoRefreshFilter((ClaimConfig) o);
            } catch (ServletException e) {
                java.util.logging.Logger.getLogger(this.getClass().getName())
                        .log(Level.SEVERE, "Unable to update the servlet filters following configuration change", e);
            }
        }
    }
}
