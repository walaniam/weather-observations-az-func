package walaniam.weather.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class WeatherDataView {

    private String dateTime;
    private float outsideTemperature;
    private float insideTemperature;
    private float pressureHpa;
}
