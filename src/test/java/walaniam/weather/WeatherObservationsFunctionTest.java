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

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@Testcontainers
public class WeatherObservationsFunctionTest {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
            DockerImageName.parse("mongo").withTag("4.2.22")
    );

    private final ExecutionContext executionContext = mock(ExecutionContext.class);
    private WeatherObservationsFunction underTest;

    @BeforeEach
    public void beforeEach() {
        doReturn(Logger.getGlobal()).when(executionContext).getLogger();
        underTest = new WeatherObservationsFunction(MONGO_DB_CONTAINER.getConnectionString());
    }

    @Test
    public void testHttpTriggerJava() {
        // Setup
        @SuppressWarnings("unchecked")
        HttpRequestMessage<String> req = mock(HttpRequestMessage.class);

        doReturn("20220904 122300,16.5,20.5,998").when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        HttpResponseMessage response = underTest.run(req, executionContext);

        // Verify
        assertEquals(response.getStatus(), HttpStatus.OK);
    }
}
