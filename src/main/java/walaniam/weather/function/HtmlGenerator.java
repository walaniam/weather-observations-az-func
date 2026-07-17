package walaniam.weather.function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class HtmlGenerator {

    private static final String CHART_PAGE_TEMPLATE = "/chart-page.html";

    public static String chartPage() throws IOException {
        try (InputStream template = HtmlGenerator.class.getResourceAsStream(CHART_PAGE_TEMPLATE)) {
            if (template == null) {
                throw new IOException("Template not found: " + CHART_PAGE_TEMPLATE);
            }
            return new String(template.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
