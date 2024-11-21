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
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import walaniam.weather.persistence.WeatherData;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ChartGenerator {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Warsaw");

    public static byte[] createChart(List<WeatherData> weatherData) throws IOException {
        // Create TimeSeries for Outside Temperature and Pressure
        TimeSeries outsideTemperatureSeries = new TimeSeries("Outside Temperature (°C)");
        TimeSeries pressureSeries = new TimeSeries("Pressure (hPa)");

        for (WeatherData data : weatherData) {
            Second timePoint = new Second(
                java.util.Date.from(data.getDateTime().atZone(ZONE_ID).toInstant())
            );
            outsideTemperatureSeries.add(timePoint, data.getOutsideTemperature());
            pressureSeries.add(timePoint, data.getPressureHpa());
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
