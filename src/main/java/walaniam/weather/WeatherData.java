package walaniam.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import static walaniam.weather.common.time.DateTimeUtils.fromUtcString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class WeatherData {

    private ZonedDateTime dateTime;
    private String outsideTemperature;
    private String insideTemperature;
    private String pressureHpa;

    public static WeatherData of(String csv) {

        var data =  Pattern.compile(",").splitAsStream(csv)
                .map(String::trim)
                .toArray(size -> new String[size]);

        if (data.length != 4) {
            throw new IllegalArgumentException("Incorrect csv: " + csv);
        }

        return WeatherData.builder()
                .dateTime(fromUtcString(data[0]))
                .outsideTemperature(data[1])
                .insideTemperature(data[2])
                .pressureHpa(data[3])
                .build();
    }
}
