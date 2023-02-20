package walaniam.weather;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.mongodb.MongoException;
import lombok.RequiredArgsConstructor;
import walaniam.weather.mongo.WeatherData;
import walaniam.weather.mongo.WeatherDataMongoRepository;
import walaniam.weather.mongo.WeatherDataRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static walaniam.weather.common.logging.LoggingUtils.info;

@RequiredArgsConstructor
public class WeatherObservationsFunctionsHandler {

    private final Function<ExecutionContext, WeatherDataRepository> repositoryProvider;

    @SuppressWarnings("unused")
    public WeatherObservationsFunctionsHandler() {
        this(System.getenv("CosmosDBConnectionString"));
    }

    public WeatherObservationsFunctionsHandler(String connectionString) {
        this(context -> new WeatherDataMongoRepository(context, connectionString));
    }

    @FunctionName("post-observations-v1")
    public HttpResponseMessage postObservation(
            @HttpTrigger(name = "req", methods = HttpMethod.POST, authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<String> request,
            ExecutionContext context) {

        String body = request.getBody();

        info(context, "Observation body data: [%s]", body);

        if (isBlank(body)) {
            return responseOf(request, HttpStatus.BAD_REQUEST, Optional.of("Missing body"));
        } else {
            WeatherDataRepository repository = repositoryProvider.apply(context);
            var data = WeatherData.of(body);
            try {
                repository.save(data);
                return responseOf(request, HttpStatus.OK, Optional.empty());
            } catch (MongoException e) {
                return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
            }
        }
    }

    @FunctionName("get-latest-observations-v1")
    public HttpResponseMessage getLatest(
            @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<String> request,
            ExecutionContext context) {

        info(context, "Getting latest observations");

        WeatherDataRepository repository = repositoryProvider.apply(context);
        try {
            List<WeatherData> latest = repository.getLatest();
            return responseOf(request, HttpStatus.OK, Optional.of(latest));
        } catch (MongoException e) {
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        }
    }

    private static <T> HttpResponseMessage responseOf(HttpRequestMessage<String> request,
                                                      HttpStatus status,
                                                      Optional<T> message) {
        HttpResponseMessage.Builder builder = request.createResponseBuilder(status);
        message.ifPresent(builder::body);
        return builder.build();
    }
}
