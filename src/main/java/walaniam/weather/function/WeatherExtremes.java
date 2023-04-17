package walaniam.weather.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class WeatherExtremes {
    private String filter;
    private WeatherDataView minOutsideTemperature;
    private WeatherDataView maxOutsideTemperature;
    private WeatherDataView minPressure;
    private WeatherDataView maxPressure;
}
