package util;

import org.junit.Assert;
import org.junit.Test;
import play.api.http.MediaRange;
import play.api.mvc.Request;
import play.i18n.Lang;
import play.libs.typedmap.TypedKey;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;

import java.security.cert.X509Certificate;
import java.util.*;

public class RequestVerifierTest {

    @Test
    public void testValidBody() {
        Map<String, List<String>> headerValues = new TreeMap<>();
        headerValues.put("X-Slack-Request-Timestamp", new ArrayList<>(Arrays.asList("1578867626")));
        headerValues.put("X-Slack-Signature", new ArrayList<>(
                Arrays.asList("v0=2eb43c3c881566ed2a5d6d158e617bdf49b6b4acf9cedd688dcc6638d0924fb5")));

        Http.Request request = new FakeRequest() {
            @Override
            public Http.RequestBody body() {
                return new Http.RequestBody("{\"token\":\"valid_token_123\",\"type\":\"event_callback\"," +
                        "\"event\":{\"text\":\"she's so quiet\",\"user\":\"USER123\",\"ts\":null}}");
            }

            @Override
            public Http.Headers getHeaders() {
                return new Http.Headers(headerValues);
            }
        };
        Assert.assertTrue(RequestVerifier.verified(request,  "signing_secret", "", ""));
    }

    @Test
    public void testNullBody() {
        Map<String, List<String>> headerValues = new TreeMap<>();
        headerValues.put("X-Slack-Request-Timestamp", new ArrayList<>(Arrays.asList("1578867626")));
        headerValues.put("X-Slack-Signature", new ArrayList<>(
                Arrays.asList("v0=c248bfcad41d7d6e3c51c4affd27b273785dccae6378afa6ad58c45ee4a81282")));

        Http.Request request = new FakeRequest() {
            @Override
            public Http.RequestBody body() {
                return null;
            }

            @Override
            public Http.Headers getHeaders() {
                return new Http.Headers(headerValues);
            }
        };
        Assert.assertTrue(RequestVerifier.verified(request, "signing_secret", "", ""));
    }

    // Leaving this here below for readability
    private abstract class FakeRequest implements Http.Request {
        public Http.Request withBody(Http.RequestBody body) { return null; }
        public Http.Request withAttrs(TypedMap newAttrs) { return null; }
        public <A> Http.Request addAttr(TypedKey<A> key, A value) { return null; }
        public Http.Request removeAttr(TypedKey<?> key) { return null; }
        public Request<Http.RequestBody> asScala() { return null; }
        public String uri() { return null; }
        public String method() { return null; }
        public String version() { return null; }
        public String remoteAddress() { return null; }
        public boolean secure() { return false; }
        public TypedMap attrs() { return null; }
        public String host() { return null; }
        public String path() { return null; }
        public List<Lang> acceptLanguages() { return null; }
        public List<MediaRange> acceptedTypes() { return null; }
        public boolean accepts(String mimeType) { return false; }
        public Map<String, String[]> queryString() { return null; }
        public String getQueryString(String key) { return null; }
        public Optional<String> queryString(String key) { return Optional.empty(); }
        public Http.Cookies cookies() { return null; }
        public Http.Cookie cookie(String name) { return null; }
        public Optional<Http.Cookie> getCookie(String name) { return Optional.empty(); }
        public boolean hasBody() { return false; }
        public Optional<String> contentType() { return Optional.empty(); }
        public Optional<String> charset() { return Optional.empty(); }
        public Optional<List<X509Certificate>> clientCertificateChain() { return Optional.empty(); }
    }
}
