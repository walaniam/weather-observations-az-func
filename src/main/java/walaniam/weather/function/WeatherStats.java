package walaniam.weather.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class WeatherStats {

    private long count;
    private String from;
    private String to;
    private MetricStats outsideTemperature;
    private MetricStats insideTemperature;
    private MetricStats pressureHpa;
    private Float avgInsideOutsideDelta;
    private Float pressureTendencyHpaPer3h;
    private String pressureTendency;
    private List<DailySummary> dailySummaries;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class MetricStats {
        private float min;
        private float max;
        private float avg;
        private float stdDev;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class DailySummary {
        private String date;
        private float minOutsideTemperature;
        private float avgOutsideTemperature;
        private float maxOutsideTemperature;
        private float amplitude;
    }
}
