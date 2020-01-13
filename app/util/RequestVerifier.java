package util;

import org.apache.commons.codec.binary.Hex;
import play.mvc.Http;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;

public class RequestVerifier {

    private static String ALGORITHM = "HmacSHA256";

    public static boolean verified(AppConfig config, Http.Request request) {

        var slackSignature = request.header("X-Slack-Signature");
        var timestamp = request.header("X-Slack-Request-Timestamp");

        if (slackSignature.isEmpty() || timestamp.isEmpty()) {
            return false;
        }

        var body = "";
        try {
            body = new String(request.body().asBytes().toArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        var secret = config.getSigningSecret();
        var baseString = String.join(":", "v0", timestamp.get(), body );
        var secretKey = new SecretKeySpec(secret.getBytes(), ALGORITHM);

        try {
            var sha256_HMAC = Mac.getInstance(ALGORITHM);
            sha256_HMAC.init(secretKey);
            var hash = "v0=" + Hex.encodeHexString(sha256_HMAC.doFinal(baseString.getBytes()));

            return hash.equals(slackSignature) ? true : false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
