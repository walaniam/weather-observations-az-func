package walaniam.weather;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import walaniam.weather.common.time.DateTimeUtils;
import walaniam.weather.function.WeatherDataView;
import walaniam.weather.function.WeatherObservationsFunctionsHandler;
import walaniam.weather.function.WeatherStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@Testcontainers
class WeatherObservationsFunctionsHandlerTest {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
            DockerImageName.parse("mongo").withTag("4.2.22")
    );

    private final ExecutionContext executionContext = mock(ExecutionContext.class);
    private WeatherObservationsFunctionsHandler underTest;

    @BeforeEach
    public void beforeEach() {
        doReturn(Logger.getGlobal()).when(executionContext).getLogger();
        underTest = new WeatherObservationsFunctionsHandler(MONGO_DB_CONTAINER.getConnectionString());
    }

    @Test
    void shouldPostObservation() {
        // Setup
        @SuppressWarnings("unchecked")
        HttpRequestMessage<String> req = mock(HttpRequestMessage.class);

        doReturn("20220904 122300,16.5,20.5,998").when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        HttpResponseMessage response = underTest.postObservation(req, executionContext);

        // Verify
        assertEquals(response.getStatus(), HttpStatus.OK);
    }

    @Test
    void shouldPostAndReadObservations() {

        var requestMessage = mock(HttpRequestMessage.class);
        mockResponseBuilderOf(requestMessage);

        IntStream.range(0, 100).forEach(index -> {
            String outTemp = String.valueOf(10 + index / 10f);
            String inTemp = String.valueOf(20 + index / 10f);
            var observation = String.format("%s,%s,%s,998", "ignored", outTemp, inTemp);

            doReturn(observation).when(requestMessage).getBody();

            // Invoke
            HttpResponseMessage response = underTest.postObservation(requestMessage, executionContext);

            // Verify
            assertEquals(response.getStatus(), HttpStatus.OK);
        });

        reset(requestMessage);
        mockResponseBuilderOf(requestMessage);

        HttpResponseMessage responseMessage = underTest.getLatest(requestMessage, executionContext);
        List<WeatherDataView> latestObservations = (List<WeatherDataView>) responseMessage.getBody();
        assertThat(latestObservations).hasSize(10);
        assertThat(latestObservations.get(0).getOutsideTemperature()).isEqualTo(19.9f);
        assertThat(latestObservations.get(9).getOutsideTemperature()).isEqualTo(19.0f);
    }

    @Test
    void shouldServeChartPage() {
        var requestMessage = mock(HttpRequestMessage.class);
        mockResponseBuilderOf(requestMessage);

        HttpResponseMessage response = underTest.getChart(requestMessage, executionContext);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("text/html", response.getHeader("Content-Type"));
        String html = (String) response.getBody();
        assertThat(html).contains("Weather Dashboard");
        assertThat(html).contains("get-chart-image-v1");
        assertThat(html).contains("get-extremes-v1");
        assertThat(html).contains("get-stats-v1");
        assertThat(html).contains("nav-prev");
        assertThat(html).contains("nav-next");
    }

    @Test
    void shouldGetChartImage() {
        var requestMessage = mock(HttpRequestMessage.class);
        mockResponseBuilderOf(requestMessage);

        doReturn("ignored,16.5,20.5,998").when(requestMessage).getBody();
        assertEquals(HttpStatus.OK, underTest.postObservation(requestMessage, executionContext).getStatus());

        reset(requestMessage);
        mockResponseBuilderOf(requestMessage);
        doReturn(Map.of()).when(requestMessage).getQueryParameters();

        HttpResponseMessage response = underTest.getChartImage(requestMessage, executionContext);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("image/png", response.getHeader("Content-Type"));
        byte[] pngBytes = (byte[]) response.getBody();
        assertThat(pngBytes).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    void shouldReturnNotFoundForChartImageOfEmptyRange() {
        var requestMessage = mock(HttpRequestMessage.class);
        mockResponseBuilderOf(requestMessage);
        doReturn(Map.of("fromDate", "20000101_000000", "toDate", "20000102_000000"))
            .when(requestMessage).getQueryParameters();

        HttpResponseMessage response = underTest.getChartImage(requestMessage, executionContext);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    void shouldGetStats() {
        var requestMessage = mock(HttpRequestMessage.class);
        mockResponseBuilderOf(requestMessage);

        doReturn("ignored,12.0,21.0,1002").when(requestMessage).getBody();
        assertEquals(HttpStatus.OK, underTest.postObservation(requestMessage, executionContext).getStatus());
        doReturn("ignored,14.0,23.0,1004").when(requestMessage).getBody();
        assertEquals(HttpStatus.OK, underTest.postObservation(requestMessage, executionContext).getStatus());

        reset(requestMessage);
        mockResponseBuilderOf(requestMessage);
        doReturn(Map.of()).when(requestMessage).getQueryParameters();

        HttpResponseMessage response = underTest.getStats(requestMessage, executionContext);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));
        WeatherStats stats = (WeatherStats) response.getBody();
        assertThat(stats.getCount()).isGreaterThanOrEqualTo(2);
        assertThat(stats.getOutsideTemperature().getMin())
            .isLessThanOrEqualTo(stats.getOutsideTemperature().getAvg());
        assertThat(stats.getOutsideTemperature().getAvg())
            .isLessThanOrEqualTo(stats.getOutsideTemperature().getMax());
        assertThat(stats.getInsideTemperature()).isNotNull();
        assertThat(stats.getPressureHpa()).isNotNull();
        assertThat(stats.getDailySummaries()).isNotEmpty();
    }

    @Test
    void shouldGetEmptyStatsForEmptyRange() {
        var requestMessage = mock(HttpRequestMessage.class);
        mockResponseBuilderOf(requestMessage);
        doReturn(Map.of("fromDate", "20000101_000000", "toDate", "20000102_000000"))
            .when(requestMessage).getQueryParameters();

        HttpResponseMessage response = underTest.getStats(requestMessage, executionContext);

        assertEquals(HttpStatus.OK, response.getStatus());
        WeatherStats stats = (WeatherStats) response.getBody();
        assertThat(stats.getCount()).isZero();
    }

    private static void mockResponseBuilderOf(HttpRequestMessage requestMessage) {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(requestMessage).createResponseBuilder(any(HttpStatus.class));
    }
}
