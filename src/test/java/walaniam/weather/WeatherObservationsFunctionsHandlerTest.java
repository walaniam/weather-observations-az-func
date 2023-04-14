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

import java.time.LocalDateTime;
import java.util.List;
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
    public void shouldPostObservation() {
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
    public void shouldPostAndReadObservations() {

        var requestMessage = mock(HttpRequestMessage.class);
        mockResponseBuilderOf(requestMessage);

        IntStream.range(0, 100).forEach(index -> {
            var dateTime = LocalDateTime.of(2100, 02, 02, 10, 10).plusHours(index);
            String outTemp = String.valueOf(10 + index / 10f);
            String inTemp = String.valueOf(20 + index / 10f);
            var observation = String.format("%s,%s,%s,998", DateTimeUtils.toUtcString(dateTime), outTemp, inTemp);

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

    private static void mockResponseBuilderOf(HttpRequestMessage requestMessage) {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(requestMessage).createResponseBuilder(any(HttpStatus.class));
    }
}
