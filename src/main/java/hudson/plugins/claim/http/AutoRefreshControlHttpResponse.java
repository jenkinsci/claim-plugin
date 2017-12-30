package hudson.plugins.claim.http;

import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Adds a header to the underlying {@link HttpServletResponse} by deferring Refresh header addition.
 * Header will be kept until the output will be written.
 * It is possible to prevent its addition even after it has been added
 */
final class AutoRefreshControlHttpResponse extends HttpServletResponseWrapper {

    private final List<String> refreshHeaderValues = Lists.newArrayList();
    private final String noRefreshHeader;
    private boolean isRefreshHeaderRemoved = false;

    AutoRefreshControlHttpResponse(HttpServletResponse response, String noRefreshHeader) {
        super(response);
        this.noRefreshHeader = noRefreshHeader;
    }

    @Override
    public void addHeader(String name, String value) {
        addHeader(name, value, true);
    }

    private void addHeader(String name, String value, boolean suppress) {
        if (name.equals(noRefreshHeader)) {
            isRefreshHeaderRemoved = true;
        } else if (suppress && name.equalsIgnoreCase(HttpHeaders.REFRESH)) {
            refreshHeaderValues.add(value);
        } else {
            super.addHeader(name, value);
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        addRefreshHeadersIfNeeded();
        return super.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        addRefreshHeadersIfNeeded();
        return super.getWriter();
    }

    private void addRefreshHeadersIfNeeded() {
        if (!isRefreshHeaderRemoved) {
            refreshHeaderValues.forEach(value -> addHeader(HttpHeaders.REFRESH, value, false));
        }
    }
}
