package walaniam.weather.function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import walaniam.weather.persistence.WeatherData;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class SensorHealthCalculator {

    private static final DateTimeFormatter LAST_SEEN_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static SensorHealthView calculate(List<WeatherData> latest, int staleMinutes, LocalDateTime nowUtc) {

        if (latest.isEmpty()) {
            return SensorHealthView.builder()
                .stale(true)
                .lastSeen(null)
                .minutesSinceLast(null)
                .staleMinutes(staleMinutes)
                .build();
        }

        LocalDateTime lastSeen = latest.get(0).getDateTime();
        long minutesSinceLast = Math.max(0, Duration.between(lastSeen, nowUtc).toMinutes());
        boolean stale = minutesSinceLast >= staleMinutes;

        return SensorHealthView.builder()
            .stale(stale)
            .lastSeen(lastSeen.format(LAST_SEEN_FORMATTER))
            .minutesSinceLast(minutesSinceLast)
            .staleMinutes(staleMinutes)
            .build();
    }
}
