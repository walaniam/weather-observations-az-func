package walaniam.weather.function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import walaniam.weather.persistence.WeatherData;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ChartGenerator {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Warsaw");

    public static byte[] createChart(List<WeatherData> weatherData) throws IOException {
        return createChart(weatherData, 0);
    }

    /**
     * @param smoothingWindowHours moving average window in hours; 0 or less means raw data
     */
    public static byte[] createChart(List<WeatherData> weatherData, int smoothingWindowHours) throws IOException {
        // Create TimeSeries for Outside Temperature and Pressure
        TimeSeries outsideTemperatureSeries = new TimeSeries("Outside Temperature (°C)");
        TimeSeries pressureSeries = new TimeSeries("Pressure (hPa)");

        for (WeatherData data : weatherData) {
            Second timePoint = new Second(
                java.util.Date.from(data.getDateTime().atZone(ZONE_ID).toInstant())
            );
            outsideTemperatureSeries.addOrUpdate(timePoint, data.getOutsideTemperature());
            pressureSeries.addOrUpdate(timePoint, data.getPressureHpa());
        }

        if (smoothingWindowHours > 0) {
            // periodCount is in units of the series' time period (Second)
            int windowSeconds = smoothingWindowHours * 3600;
            outsideTemperatureSeries = MovingAverage.createMovingAverage(
                outsideTemperatureSeries, "Outside Temperature (°C)", windowSeconds, 0);
            pressureSeries = MovingAverage.createMovingAverage(
                pressureSeries, "Pressure (hPa)", windowSeconds, 0);
        }

        // Create a dataset for outside temperature
        TimeSeriesCollection temperatureDataset = new TimeSeriesCollection();
        temperatureDataset.addSeries(outsideTemperatureSeries);

        // Create the primary chart with temperature dataset and default axis settings
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Weather Data",
            "Time",
            "Outside Temperature (°C)",
            temperatureDataset,
            true,
            true,
            false
        );

        // Customize plot and renderer for temperature series
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer temperatureRenderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(temperatureRenderer);
        temperatureRenderer.setSeriesPaint(0, Color.RED);   // Outside Temperature color

        // Set up the X-axis as a DateAxis with custom tick units
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setTickUnit(
            new DateTickUnit(DateTickUnitType.HOUR, 6),
            true,
            false
        );

        // Add a secondary axis for pressure
        NumberAxis pressureAxis = new NumberAxis("Pressure (hPa)");
        pressureAxis.setRange(
            roundDownTo10(weatherData.stream().mapToDouble(WeatherData::getPressureHpa).min().orElse(900)),
            roundUpTo10(weatherData.stream().mapToDouble(WeatherData::getPressureHpa).max().orElse(1050))
        );
        plot.setRangeAxis(1, pressureAxis);  // Adds a secondary range axis on the right
        plot.mapDatasetToRangeAxis(1, 1);    // Maps the second dataset to the right axis

        // Create a dataset and renderer for pressure
        TimeSeriesCollection pressureDataset = new TimeSeriesCollection();
        pressureDataset.addSeries(pressureSeries);
        plot.setDataset(1, pressureDataset);

        XYLineAndShapeRenderer pressureRenderer = new XYLineAndShapeRenderer(true, false);
        pressureRenderer.setSeriesPaint(0, Color.BLUE);  // Pressure color
        plot.setRenderer(1, pressureRenderer);

        return chartToPngBytes(chart, 1200, 675);
    }

    /**
     * Daily trend chart: average outside temperature line with a min-max deviation band
     * and average pressure line, one data point per day.
     */
    public static byte[] createTrendChart(List<WeatherData> weatherData) throws IOException {
        Map<LocalDate, List<WeatherData>> byDay = weatherData.stream()
            .collect(Collectors.groupingBy(
                data -> data.getDateTime().toLocalDate(),
                TreeMap::new,
                Collectors.toList()
            ));

        YIntervalSeries temperatureSeries = new YIntervalSeries("Avg Outside Temperature (°C), min-max band");
        TimeSeries pressureSeries = new TimeSeries("Avg Pressure (hPa)");

        byDay.forEach((day, observations) -> {
            double avgTemp = observations.stream().mapToDouble(WeatherData::getOutsideTemperature).average().orElseThrow();
            double minTemp = observations.stream().mapToDouble(WeatherData::getOutsideTemperature).min().orElseThrow();
            double maxTemp = observations.stream().mapToDouble(WeatherData::getOutsideTemperature).max().orElseThrow();
            double avgPressure = observations.stream().mapToDouble(WeatherData::getPressureHpa).average().orElseThrow();

            long middayMillis = java.util.Date.from(
                LocalDateTime.of(day, java.time.LocalTime.NOON).atZone(ZONE_ID).toInstant()
            ).getTime();
            temperatureSeries.add(middayMillis, avgTemp, minTemp, maxTemp);
            pressureSeries.addOrUpdate(new Day(day.getDayOfMonth(), day.getMonthValue(), day.getYear()), avgPressure);
        });

        YIntervalSeriesCollection temperatureDataset = new YIntervalSeriesCollection();
        temperatureDataset.addSeries(temperatureSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Weather Trends (daily)",
            "Time",
            "Outside Temperature (°C)",
            temperatureDataset,
            true,
            true,
            false
        );

        XYPlot plot = chart.getXYPlot();

        DeviationRenderer temperatureRenderer = new DeviationRenderer(true, true);
        temperatureRenderer.setSeriesPaint(0, Color.RED);
        temperatureRenderer.setSeriesFillPaint(0, new Color(255, 0, 0, 40));
        temperatureRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(temperatureRenderer);

        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setTickUnit(
            new DateTickUnit(DateTickUnitType.DAY, 1),
            true,
            false
        );

        NumberAxis pressureAxis = new NumberAxis("Pressure (hPa)");
        pressureAxis.setRange(
            roundDownTo10(weatherData.stream().mapToDouble(WeatherData::getPressureHpa).min().orElse(900)),
            roundUpTo10(weatherData.stream().mapToDouble(WeatherData::getPressureHpa).max().orElse(1050))
        );
        plot.setRangeAxis(1, pressureAxis);
        plot.mapDatasetToRangeAxis(1, 1);

        TimeSeriesCollection pressureDataset = new TimeSeriesCollection();
        pressureDataset.addSeries(pressureSeries);
        plot.setDataset(1, pressureDataset);

        XYLineAndShapeRenderer pressureRenderer = new XYLineAndShapeRenderer(true, true);
        pressureRenderer.setSeriesPaint(0, Color.BLUE);
        pressureRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(1, pressureRenderer);

        return chartToPngBytes(chart, 1200, 675);
    }

    private static byte[] chartToPngBytes(JFreeChart chart, int width, int height) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(byteArrayOutputStream, chart, width, height);
        return byteArrayOutputStream.toByteArray();
    }

    private static double roundDownTo10(double number) {
        return Math.floor(number / 10) * 10;
    }

    private static double roundUpTo10(double number) {
        return Math.ceil(number / 10) * 10;
    }
}
