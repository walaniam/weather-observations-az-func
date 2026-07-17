package walaniam.weather.function;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.junit.jupiter.api.Test;
import walaniam.weather.persistence.WeatherData;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class ChartGeneratorTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Test
    void shouldUseRequestedZoneForRawChartAxis() {
        ZoneId warsaw = ZoneId.of("Europe/Warsaw");
        List<WeatherData> observations = List.of(observation(BASE, 10, 20, 1000), observation(BASE.plusHours(1), 11, 20, 1010));

        JFreeChart chart = ChartGenerator.buildChart(observations, 0, warsaw);

        assertThat(axisTimeZone(chart)).isEqualTo(TimeZone.getTimeZone(warsaw));
    }

    @Test
    void shouldDefaultToUtcForRawChartAxis() {
        List<WeatherData> observations = List.of(observation(BASE, 10, 20, 1000), observation(BASE.plusHours(1), 11, 20, 1010));

        JFreeChart chart = ChartGenerator.buildChart(observations, 0, ZoneOffset.UTC);

        assertThat(axisTimeZone(chart)).isEqualTo(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @Test
    void shouldUseRequestedZoneForTrendChartAxis() {
        ZoneId warsaw = ZoneId.of("Europe/Warsaw");
        List<WeatherData> observations = List.of(
            observation(BASE, 10, 20, 1000),
            observation(BASE.plusDays(1), 12, 20, 1010)
        );

        JFreeChart chart = ChartGenerator.buildTrendChart(observations, warsaw);

        assertThat(axisTimeZone(chart)).isEqualTo(TimeZone.getTimeZone(warsaw));
    }

    private static TimeZone axisTimeZone(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        return dateAxis.getTimeZone();
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
