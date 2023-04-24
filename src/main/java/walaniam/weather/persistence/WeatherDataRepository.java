package walaniam.weather.persistence;

import walaniam.weather.function.WeatherExtremes;

import java.util.List;

public interface WeatherDataRepository {

    void save(WeatherData data);
    List<WeatherData> getLatest(int limit);

    default List<WeatherData> getLatest() {
        return getLatest(10);
    }

    WeatherExtremes getExtremes(Integer fromDays, Integer toDays);
}
