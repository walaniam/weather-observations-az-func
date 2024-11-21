package walaniam.weather.function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import walaniam.weather.common.time.DateTimeUtils;
import walaniam.weather.persistence.WeatherData;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class HtmlGenerator {

    public static String generateHtmlWithImage(List<WeatherData> observations) throws IOException {

        String label = "Weather data: %s to %s".formatted(
            DateTimeUtils.toDate(observations.get(0).getDateTime()),
            DateTimeUtils.toDate(observations.get(observations.size() - 1).getDateTime())
        );
        byte[] pngBytes = ChartGenerator.createChart(observations);

        String base64Image = Base64.getEncoder().encodeToString(pngBytes);
        String htmlContent = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                      .right-align {
                        text-align: right;
                      }
                    </style>                    
                    <title>Weather Data Chart</title>
                </head>
                <body>
                    <h1>%s</h1>
                    <img src="data:image/png;base64,%s" alt="Weather Chart"/>
                    <table>
                    <tr>
                        <td>Min temperature</td>
                        <td class="right-align">%s</td>
                    </tr>
                    <tr>
                        <td>Max temperature</td>
                        <td class="right-align">%s</td>
                    </tr>
                    <tr>
                        <td>Min pressure</td>
                        <td class="right-align">%s</td>
                    </tr>
                    <tr>
                        <td>Max pressure</td>
                        <td class="right-align">%s</td>
                    </tr>
                    </table>
                </body>
                </html>
                """
            .formatted(
                label,
                base64Image,
                minFloatFormatted(observations, WeatherData::getOutsideTemperature),
                maxFloatFormatted(observations, WeatherData::getOutsideTemperature),
                minFloatFormatted(observations, WeatherData::getPressureHpa),
                maxFloatFormatted(observations, WeatherData::getPressureHpa)
            );

        return htmlContent;
    }

    private static String minFloatFormatted(Collection<WeatherData> data, ToDoubleFunction<WeatherData> toDoubleFunction) {
        OptionalDouble min = data.stream().mapToDouble(toDoubleFunction).min();
        return min.isPresent()
            ? String.format("%.2f", min.getAsDouble())
            : "n/a";
    }

    private static String maxFloatFormatted(Collection<WeatherData> data, ToDoubleFunction<WeatherData> toDoubleFunction) {
        OptionalDouble max = data.stream().mapToDouble(toDoubleFunction).max();
        return max.isPresent()
            ? String.format("%.2f", max.getAsDouble())
            : "n/a";
    }
}
