package walaniam.weather.common.time;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateTimeUtils {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static LocalDateTime fromUtcString(String dateTime) {
        return LocalDateTime.parse(dateTime, DT_FORMATTER).atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    public static String toDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dateTime.format(formatter);
    }
}
