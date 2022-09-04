package walaniam.weather;

import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Unit test for Function class.
 */
public class WeatherObservationsFunctionTest {
    /**
     * Unit test for HttpTriggerJava method.
     */
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

        OutputBinding<WeatherData> document = mock(OutputBinding.class);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        HttpResponseMessage response = new WeatherObservationsFunction().run(req, document, context);

        // Verify
        assertEquals(response.getStatus(), HttpStatus.OK);
    }
}
