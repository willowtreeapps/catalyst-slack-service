package util;

import org.apache.commons.codec.binary.Hex;
import play.mvc.Http;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class RequestVerifier {

    private static String ALGORITHM = "HmacSHA256";

    /**
     * Compares the header value of X-Slack-Signature with the hash value of the X-Slack-Request-Timestamp
     * and the request body, using the SLACK_SIGNING_SECRET as the secret key
     * @param config
     * @param request
     * @return
     */
    public static boolean verified(AppConfig config, Http.Request request) {

        var slackSignature = request.header("X-Slack-Signature");
        var timestamp = request.header("X-Slack-Request-Timestamp");

        if (slackSignature.isEmpty() || timestamp.isEmpty()) {
            return false;
        }

        var secret = config.getSigningSecret();
        var secretKey = new SecretKeySpec(secret.getBytes(), ALGORITHM);

        try {
            var body = new String(request.body().asBytes().toArray(), "UTF-8");
            var baseString = String.join(":", "v0", timestamp.get(), body );
            var sha256_HMAC = Mac.getInstance(ALGORITHM);
            sha256_HMAC.init(secretKey);
            var hash = "v0=" + Hex.encodeHexString(sha256_HMAC.doFinal(baseString.getBytes()));

            return hash.equals(slackSignature.get()) ? true : false;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return false;
    }
}
