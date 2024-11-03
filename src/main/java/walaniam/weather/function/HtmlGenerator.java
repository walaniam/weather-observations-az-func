package walaniam.weather.function;

import java.util.Base64;

class HtmlGenerator {

    public static String generateHtmlWithImage(String label, byte[] pngBytes) {
        String base64Image = Base64.getEncoder().encodeToString(pngBytes);
        String htmlContent = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Weather Data Chart</title>
                </head>
                <body>
                    <h1>%s</h1>
                    <img src="data:image/png;base64,%s" alt="Weather Chart"/>
                </body>
                </html>
                """.formatted(label, base64Image);

        return htmlContent;
    }
}
