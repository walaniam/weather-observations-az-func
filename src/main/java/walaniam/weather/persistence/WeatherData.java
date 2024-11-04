package walaniam.weather.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class WeatherData {

    private LocalDateTime dateTime;
    private float outsideTemperature;
    private float insideTemperature;
    private float pressureHpa;

    public static WeatherData of(String csv) {

        String[] data =  Pattern.compile(",").splitAsStream(csv)
                .map(String::trim)
                .toArray(String[]::new);

        if (data.length != 4) {
            throw new IllegalArgumentException("Incorrect csv: " + csv);
        }

        // ignore data[0] which represents LocalDateTime of remote sensor
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(
            Instant.now().truncatedTo(ChronoUnit.SECONDS),
            ZoneOffset.UTC
        );
        return WeatherData.builder()
                .dateTime(utcDateTime)
                .outsideTemperature(Float.parseFloat(data[1]))
                .insideTemperature(Float.parseFloat(data[2]))
                .pressureHpa(Float.parseFloat(data[3]))
                .build();
    }
}
