package walaniam.weather.common.time;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateTimeUtils {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");

    public static ZonedDateTime fromUtcString(String dateTime) {
        var local = LocalDateTime.parse(dateTime, DT_FORMATTER);
        return ZonedDateTime.of(local, ZoneId.of("UTC"));
    }
}
