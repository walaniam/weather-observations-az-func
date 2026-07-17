package walaniam.weather.function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import walaniam.weather.common.time.DateTimeUtils;
import walaniam.weather.persistence.WeatherData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class WeatherStatsCalculator {

    private static final int TENDENCY_HOURS = 3;
    private static final float STEADY_THRESHOLD_HPA = 1.0f;

    /**
     * @param observations observations sorted ascending by dateTime
     * @param zone         timezone used to group observations into local calendar days for
     *                     {@code dailySummaries} (observation timestamps are stored as UTC)
     */
    public static WeatherStats calculate(List<WeatherData> observations, ZoneId zone) {

        if (observations.isEmpty()) {
            return WeatherStats.builder()
                .count(0)
                .build();
        }

        WeatherData first = observations.get(0);
        WeatherData last = observations.get(observations.size() - 1);

        float avgDelta = (float) observations.stream()
            .mapToDouble(o -> o.getInsideTemperature() - o.getOutsideTemperature())
            .average()
            .orElse(0);

        Float tendency = pressureTendency(observations);

        return WeatherStats.builder()
            .count(observations.size())
            .from(String.valueOf(first.getDateTime()))
            .to(String.valueOf(last.getDateTime()))
            .outsideTemperature(metricStatsOf(observations, WeatherData::getOutsideTemperature))
            .insideTemperature(metricStatsOf(observations, WeatherData::getInsideTemperature))
            .pressureHpa(metricStatsOf(observations, WeatherData::getPressureHpa))
            .avgInsideOutsideDelta(avgDelta)
            .pressureTendencyHpaPer3h(tendency)
            .pressureTendency(tendencyLabel(tendency))
            .dailySummaries(dailySummariesOf(observations, zone))
            .build();
    }

    private static WeatherStats.MetricStats metricStatsOf(List<WeatherData> observations,
                                                          ToDoubleFunction<WeatherData> metric) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;
        for (WeatherData observation : observations) {
            double value = metric.applyAsDouble(observation);
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        double avg = sum / observations.size();
        double squaredDiffSum = observations.stream()
            .mapToDouble(metric)
            .map(value -> (value - avg) * (value - avg))
            .sum();
        double stdDev = Math.sqrt(squaredDiffSum / observations.size());
        return WeatherStats.MetricStats.builder()
            .min((float) min)
            .max((float) max)
            .avg((float) avg)
            .stdDev((float) stdDev)
            .build();
    }

    private static Float pressureTendency(List<WeatherData> observations) {
        WeatherData last = observations.get(observations.size() - 1);
        LocalDateTime referenceTime = last.getDateTime().minusHours(TENDENCY_HOURS);
        WeatherData reference = null;
        for (WeatherData observation : observations) {
            if (observation.getDateTime().isAfter(referenceTime)) {
                break;
            }
            reference = observation;
        }
        if (reference == null) {
            return null;
        }
        return last.getPressureHpa() - reference.getPressureHpa();
    }

    private static String tendencyLabel(Float tendency) {
        if (tendency == null) {
            return null;
        }
        if (tendency > STEADY_THRESHOLD_HPA) {
            return "RISING";
        }
        if (tendency < -STEADY_THRESHOLD_HPA) {
            return "FALLING";
        }
        return "STEADY";
    }

    private static List<WeatherStats.DailySummary> dailySummariesOf(List<WeatherData> observations, ZoneId zone) {
        TreeMap<LocalDate, List<WeatherData>> byDay = new TreeMap<>();
        observations.forEach(observation ->
            byDay.computeIfAbsent(
                observation.getDateTime().atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDate(),
                day -> new java.util.ArrayList<>())
                .add(observation));
        return byDay.entrySet().stream()
            .map(entry -> {
                WeatherStats.MetricStats outside = metricStatsOf(entry.getValue(), WeatherData::getOutsideTemperature);
                return WeatherStats.DailySummary.builder()
                    .date(DateTimeUtils.toDate(entry.getKey().atStartOfDay()))
                    .minOutsideTemperature(outside.getMin())
                    .avgOutsideTemperature(outside.getAvg())
                    .maxOutsideTemperature(outside.getMax())
                    .amplitude(outside.getMax() - outside.getMin())
                    .build();
            })
            .toList();
    }
}
