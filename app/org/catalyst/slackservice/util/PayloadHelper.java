package org.catalyst.slackservice.util;

import java.util.Arrays;
import java.util.Optional;

public class PayloadHelper {

    public static Optional<String> getFirstArrayValue(String[] array) {
        if (array == null) {
            return Optional.empty();
        }

        return Arrays.asList(array).stream().findFirst();
    }
}
