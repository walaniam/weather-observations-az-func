package walaniam.weather.function;

import org.junit.jupiter.api.Test;
import walaniam.weather.persistence.WeatherData;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WeatherStatsCalculatorTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Test
    void shouldReturnZeroCountForEmptyObservations() {
        WeatherStats stats = WeatherStatsCalculator.calculate(List.of());

        assertThat(stats.getCount()).isZero();
        assertThat(stats.getOutsideTemperature()).isNull();
        assertThat(stats.getDailySummaries()).isNull();
    }

    @Test
    void shouldCalculateMetricStats() {
        List<WeatherData> observations = List.of(
            observation(BASE, 10, 20, 1000),
            observation(BASE.plusHours(1), 20, 22, 1010),
            observation(BASE.plusHours(2), 30, 24, 1020)
        );

        WeatherStats stats = WeatherStatsCalculator.calculate(observations);

        assertThat(stats.getCount()).isEqualTo(3);
        assertThat(stats.getOutsideTemperature().getMin()).isEqualTo(10f);
        assertThat(stats.getOutsideTemperature().getMax()).isEqualTo(30f);
        assertThat(stats.getOutsideTemperature().getAvg()).isEqualTo(20f);
        assertThat(stats.getOutsideTemperature().getStdDev()).isCloseTo(8.165f, within(0.01f));
        assertThat(stats.getInsideTemperature().getAvg()).isEqualTo(22f);
        assertThat(stats.getPressureHpa().getAvg()).isEqualTo(1010f);
        assertThat(stats.getAvgInsideOutsideDelta()).isEqualTo(2f);
    }

    @Test
    void shouldCalculateRisingPressureTendency() {
        List<WeatherData> observations = List.of(
            observation(BASE, 10, 20, 1000),
            observation(BASE.plusHours(2), 10, 20, 1002),
            observation(BASE.plusHours(4), 10, 20, 1005)
        );

        WeatherStats stats = WeatherStatsCalculator.calculate(observations);

        assertThat(stats.getPressureTendencyHpaPer3h()).isEqualTo(5f);
        assertThat(stats.getPressureTendency()).isEqualTo("RISING");
    }

    @Test
    void shouldCalculateFallingPressureTendency() {
        List<WeatherData> observations = List.of(
            observation(BASE, 10, 20, 1010),
            observation(BASE.plusHours(4), 10, 20, 1005)
        );

        WeatherStats stats = WeatherStatsCalculator.calculate(observations);

        assertThat(stats.getPressureTendencyHpaPer3h()).isEqualTo(-5f);
        assertThat(stats.getPressureTendency()).isEqualTo("FALLING");
    }

    @Test
    void shouldReturnNullTendencyWhenRangeShorterThanThreeHours() {
        List<WeatherData> observations = List.of(
            observation(BASE, 10, 20, 1000),
            observation(BASE.plusHours(1), 10, 20, 1010)
        );

        WeatherStats stats = WeatherStatsCalculator.calculate(observations);

        assertThat(stats.getPressureTendencyHpaPer3h()).isNull();
        assertThat(stats.getPressureTendency()).isNull();
    }

    @Test
    void shouldCalculateDailySummaries() {
        List<WeatherData> observations = List.of(
            observation(BASE, 10, 20, 1000),
            observation(BASE.plusHours(12), 20, 20, 1000),
            observation(BASE.plusDays(1), 5, 20, 1000),
            observation(BASE.plusDays(1).plusHours(12), 15, 20, 1000)
        );

        WeatherStats stats = WeatherStatsCalculator.calculate(observations);

        assertThat(stats.getDailySummaries()).hasSize(2);
        WeatherStats.DailySummary day1 = stats.getDailySummaries().get(0);
        assertThat(day1.getDate()).isEqualTo("2026-07-01");
        assertThat(day1.getMinOutsideTemperature()).isEqualTo(10f);
        assertThat(day1.getMaxOutsideTemperature()).isEqualTo(20f);
        assertThat(day1.getAvgOutsideTemperature()).isEqualTo(15f);
        assertThat(day1.getAmplitude()).isEqualTo(10f);
        WeatherStats.DailySummary day2 = stats.getDailySummaries().get(1);
        assertThat(day2.getDate()).isEqualTo("2026-07-02");
        assertThat(day2.getAmplitude()).isEqualTo(10f);
    }

    private static WeatherData observation(LocalDateTime dateTime, float outside, float inside, float pressure) {
        return WeatherData.builder()
            .dateTime(dateTime)
            .outsideTemperature(outside)
            .insideTemperature(inside)
            .pressureHpa(pressure)
            .build();
    }
}
