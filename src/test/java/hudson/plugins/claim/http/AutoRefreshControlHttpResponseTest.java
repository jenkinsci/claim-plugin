package hudson.plugins.claim.http;

import com.google.common.net.HttpHeaders;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Arnaud TAMAILLON
 */
public class AutoRefreshControlHttpResponseTest {
    @Test
    public void addingRefreshHeaderIsDelayedUntilGetWriter() throws IOException {
        final String headerValue = "10";
        final String noRefreshHeaderMarker = "DISABLE";
        AutoRefreshControlHttpResponse refreshControlResponse = new AutoRefreshControlHttpResponse(new
                HttpResponseDummy(), noRefreshHeaderMarker);

        refreshControlResponse.addHeader(HttpHeaders.REFRESH, headerValue);
        assertThat(refreshControlResponse.containsHeader(HttpHeaders.REFRESH), is(false));

        refreshControlResponse.getWriter();
        assertThat(refreshControlResponse.containsHeader(HttpHeaders.REFRESH), is(true));
        assertThat(refreshControlResponse.getHeader(HttpHeaders.REFRESH), equalTo(headerValue));
    }

    @Test
    public void addingRefreshHeaderIsDelayedUntilGetOutputStream() throws IOException {
        final String headerValue = "10";
        final String noRefreshHeaderMarker = "DISABLE";
        AutoRefreshControlHttpResponse refreshControlResponse = new AutoRefreshControlHttpResponse(new
                HttpResponseDummy(), noRefreshHeaderMarker);

        refreshControlResponse.addHeader(HttpHeaders.REFRESH, headerValue);
        assertThat(refreshControlResponse.containsHeader(HttpHeaders.REFRESH), is(false));

        refreshControlResponse.getOutputStream();
        assertThat(refreshControlResponse.containsHeader(HttpHeaders.REFRESH), is(true));
        assertThat(refreshControlResponse.getHeader(HttpHeaders.REFRESH), equalTo(headerValue));
    }

    @Test
    public void addingRefreshHeaderIsSuppressedAtGetWriterIfMarkerHeaderIsSet() throws IOException {
        final String headerValue = "10";
        final String noRefreshHeaderMarker = "DISABLE";
        AutoRefreshControlHttpResponse refreshControlResponse = new AutoRefreshControlHttpResponse(new
                HttpResponseDummy(), noRefreshHeaderMarker);

        refreshControlResponse.addHeader(HttpHeaders.REFRESH, headerValue);
        refreshControlResponse.addHeader(noRefreshHeaderMarker, "something");

        refreshControlResponse.getWriter();
        assertThat(refreshControlResponse.containsHeader(HttpHeaders.REFRESH), is(false));
        assertThat(refreshControlResponse.containsHeader(noRefreshHeaderMarker), is(false));
    }

    @Test
    public void addingRefreshHeaderIsSuppressedAtGetOutputStreamIfMarkerHeaderIsSet() throws IOException {
        final String headerValue = "10";
        final String noRefreshHeaderMarker = "DISABLE";
        AutoRefreshControlHttpResponse refreshControlResponse = new AutoRefreshControlHttpResponse(new
                HttpResponseDummy(), noRefreshHeaderMarker);

        refreshControlResponse.addHeader(HttpHeaders.REFRESH, headerValue);
        refreshControlResponse.addHeader(noRefreshHeaderMarker, "something");

        refreshControlResponse.getOutputStream();
        assertThat(refreshControlResponse.containsHeader(HttpHeaders.REFRESH), is(false));
        assertThat(refreshControlResponse.containsHeader(noRefreshHeaderMarker), is(false));
    }

}

