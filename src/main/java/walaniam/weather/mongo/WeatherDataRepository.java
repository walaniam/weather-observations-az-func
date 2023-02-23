package walaniam.weather.mongo;

import java.util.List;

public interface WeatherDataRepository {

    void save(WeatherData data);
    List<WeatherData> getLatest(int limit);

    default List<WeatherData> getLatest() {
        return getLatest(10);
    }
}
