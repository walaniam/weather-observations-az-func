package walaniam.weather.function;

import org.junit.jupiter.api.Test;
import walaniam.weather.persistence.WeatherData;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensorHealthCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 1, 12, 0);

    @Test
    void shouldReturnStaleWithNullsForEmptyObservations() {
        SensorHealthView health = SensorHealthCalculator.calculate(List.of(), 60, NOW);

        assertThat(health.isStale()).isTrue();
        assertThat(health.getLastSeen()).isNull();
        assertThat(health.getMinutesSinceLast()).isNull();
        assertThat(health.getStaleMinutes()).isEqualTo(60);
    }

    @Test
    void shouldReturnNotStaleForFreshObservation() {
        List<WeatherData> latest = List.of(observation(NOW));

        SensorHealthView health = SensorHealthCalculator.calculate(latest, 60, NOW);

        assertThat(health.isStale()).isFalse();
        assertThat(health.getMinutesSinceLast()).isEqualTo(0);
        assertThat(health.getLastSeen()).isEqualTo("20260701_120000");
    }

    @Test
    void shouldReturnStaleForObservationOlderThanThreshold() {
        List<WeatherData> latest = List.of(observation(NOW.minusMinutes(90)));

        SensorHealthView health = SensorHealthCalculator.calculate(latest, 60, NOW);

        assertThat(health.isStale()).isTrue();
        assertThat(health.getMinutesSinceLast()).isEqualTo(90);
    }

    @Test
    void shouldTreatExactThresholdAsStale() {
        List<WeatherData> latest = List.of(observation(NOW.minusMinutes(60)));

        SensorHealthView health = SensorHealthCalculator.calculate(latest, 60, NOW);

        assertThat(health.isStale()).isTrue();
        assertThat(health.getMinutesSinceLast()).isEqualTo(60);
    }

    @Test
    void shouldRespectCustomStaleMinutes() {
        List<WeatherData> latest = List.of(observation(NOW.minusMinutes(90)));

        SensorHealthView health = SensorHealthCalculator.calculate(latest, 120, NOW);

        assertThat(health.isStale()).isFalse();
        assertThat(health.getMinutesSinceLast()).isEqualTo(90);
        assertThat(health.getStaleMinutes()).isEqualTo(120);
    }

    private static WeatherData observation(LocalDateTime dateTime) {
        return WeatherData.builder()
            .dateTime(dateTime)
            .outsideTemperature(10)
            .insideTemperature(20)
            .pressureHpa(1000)
            .build();
    }
}
