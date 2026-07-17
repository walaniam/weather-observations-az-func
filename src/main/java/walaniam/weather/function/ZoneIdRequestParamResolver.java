package walaniam.weather.function;

import com.microsoft.azure.functions.HttpRequestMessage;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.apache.commons.lang3.StringUtils.isBlank;

class ZoneIdRequestParamResolver {

    private ZoneIdRequestParamResolver() {
    }

    /**
     * Resolves the caller's timezone from the "tz" query parameter (an IANA zone id, e.g.
     * "Europe/Warsaw"). Falls back to {@link ZoneOffset#UTC} when the parameter is missing,
     * blank, or not a valid zone id, since a timezone is a display concern and should never
     * fail the request.
     */
    static ZoneId fromRequest(HttpRequestMessage<String> request) {
        String tz = request.getQueryParameters().get("tz");
        if (isBlank(tz)) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
