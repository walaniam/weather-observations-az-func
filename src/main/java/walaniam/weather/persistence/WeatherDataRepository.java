package walaniam.weather.persistence;

import walaniam.weather.function.WeatherExtremes;
import walaniam.weather.mongo.DateRange;

import java.util.List;

public interface WeatherDataRepository {

    void save(WeatherData data);
    List<WeatherData> getLatest(int limit);

    default List<WeatherData> getLatest() {
        return getLatest(10);
    }

    List<WeatherData> getRange(DateRange dateRange);

    WeatherExtremes getExtremes(DateRange dateRange);
}
