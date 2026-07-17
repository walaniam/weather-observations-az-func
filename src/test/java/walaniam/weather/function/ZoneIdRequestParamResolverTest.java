package walaniam.weather.function;

import com.microsoft.azure.functions.HttpRequestMessage;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ZoneIdRequestParamResolverTest {

    @Test
    void shouldResolveValidIanaZoneId() {
        HttpRequestMessage<String> request = requestWithTz("Europe/Warsaw");

        ZoneId zone = ZoneIdRequestParamResolver.fromRequest(request);

        assertThat(zone).isEqualTo(ZoneId.of("Europe/Warsaw"));
    }

    @Test
    void shouldFallBackToUtcWhenTzParamMissing() {
        HttpRequestMessage<String> request = mock(HttpRequestMessage.class);
        doReturn(Map.of()).when(request).getQueryParameters();

        ZoneId zone = ZoneIdRequestParamResolver.fromRequest(request);

        assertThat(zone).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldFallBackToUtcWhenTzParamBlank() {
        HttpRequestMessage<String> request = requestWithTz("   ");

        ZoneId zone = ZoneIdRequestParamResolver.fromRequest(request);

        assertThat(zone).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldFallBackToUtcWhenTzParamInvalid() {
        HttpRequestMessage<String> request = requestWithTz("Not/AZone");

        ZoneId zone = ZoneIdRequestParamResolver.fromRequest(request);

        assertThat(zone).isEqualTo(ZoneOffset.UTC);
    }

    @SuppressWarnings("unchecked")
    private static HttpRequestMessage<String> requestWithTz(String tz) {
        HttpRequestMessage<String> request = mock(HttpRequestMessage.class);
        Map<String, String> params = new HashMap<>();
        params.put("tz", tz);
        doReturn(params).when(request).getQueryParameters();
        return request;
    }
}
