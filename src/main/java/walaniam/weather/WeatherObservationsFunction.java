package walaniam.weather;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.mongodb.MongoException;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static walaniam.weather.common.logging.LoggingUtils.info;

@RequiredArgsConstructor
public class WeatherObservationsFunction {

    private final Function<ExecutionContext, WeatherDataRepository> repositoryProvider;

    public WeatherObservationsFunction() {
        this(System.getenv("CosmosDBConnectionString"));
    }

    public WeatherObservationsFunction(String connectionString) {
        this(context -> new WeatherDataMongoRepository(context, connectionString));
    }

    @FunctionName("observations-v1")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = HttpMethod.POST, authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<String> request,
            ExecutionContext context) {

        var body = request.getBody();

        info(context, "Observation body data: [%s]", body);

        if (isBlank(body)) {
            return responseOf(request, HttpStatus.BAD_REQUEST, Optional.of("Missing body"));
        } else {
            var repository = repositoryProvider.apply(context);
            var data = WeatherData.of(body);
            try {
                repository.save(data);
                return responseOf(request, HttpStatus.OK, Optional.empty());
            } catch (MongoException e) {
                return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
            }
        }
    }

    private static HttpResponseMessage responseOf(HttpRequestMessage<String> request,
                                                  HttpStatus status,
                                                  Optional<String> message) {
        var builder = request.createResponseBuilder(status);
        message.ifPresent(builder::body);
        return builder.build();
    }
}
