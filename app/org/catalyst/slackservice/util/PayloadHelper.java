package org.catalyst.slackservice.util;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PayloadHelper {

    public final static String UTF8 = "UTF-8";
    public final static Charset CHARSET_UTF8 = Charset.forName(UTF8);

    public static Map<String, String> getFormUrlEncodedRequestBody(String request) {
        List<NameValuePair> parsedRequest = URLEncodedUtils.parse(request, CHARSET_UTF8);

        return parsedRequest.parallelStream().collect(
                Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    }

    public static String getMapValue(Map<String, String> map, String key) {
        var value = map.get(key);
        return value == null ? "" : value;
    }
}
