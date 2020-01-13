package util;

import org.apache.commons.codec.binary.Hex;
import play.mvc.Http;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class RequestVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Slack-Signature";
    private static final String TIMESTAMP_HEADER = "X-Slack-Request-Timestamp";

    public static boolean headersExist(Http.Request request) {
        var slackSignature = request.header(SIGNATURE_HEADER);
        var timestamp = request.header(TIMESTAMP_HEADER);

        return !(slackSignature == null || slackSignature.isEmpty() ||
                timestamp == null || timestamp.isEmpty());
    }

    /**
     * Compares the header value of X-Slack-Signature with the hash value of the X-Slack-Request-Timestamp
     * and the request body, using the SLACK_SIGNING_SECRET as the secret key
     * @param signingSecret
     * @param request
     * @return
     */
    public static boolean verified(String signingSecret, Http.Request request) {
        var slackSignature = request.header(SIGNATURE_HEADER);
        var timestamp = request.header(TIMESTAMP_HEADER);

        var secretKey = new SecretKeySpec(signingSecret.getBytes(), ALGORITHM);
        var hash = "";
        try {
            var requestBody = request.body();
            var body = requestBody == null ? "" :
                    new String(requestBody.asBytes().toArray(), "UTF-8");
            var baseString = String.join(":", "v0", timestamp.get(), body );
            var sha256_HMAC = Mac.getInstance(ALGORITHM);
            sha256_HMAC.init(secretKey);
            hash = "v0=" + Hex.encodeHexString(sha256_HMAC.doFinal(baseString.getBytes()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            // TODO: logging
            e.printStackTrace();
        }
        return hash.equals(slackSignature.get());
    }
}
