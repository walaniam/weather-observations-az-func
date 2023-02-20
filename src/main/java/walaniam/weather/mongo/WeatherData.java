package walaniam.weather.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import static walaniam.weather.common.time.DateTimeUtils.fromUtcString;

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
                .toArray(size -> new String[size]);

        if (data.length != 4) {
            throw new IllegalArgumentException("Incorrect csv: " + csv);
        }

        return WeatherData.builder()
                .dateTime(fromUtcString(data[0]))
                .outsideTemperature(Float.parseFloat(data[1]))
                .insideTemperature(Float.parseFloat(data[2]))
                .pressureHpa(Float.parseFloat(data[3]))
                .build();
    }
}
