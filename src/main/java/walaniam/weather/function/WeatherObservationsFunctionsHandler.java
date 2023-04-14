package walaniam.weather.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.mongodb.MongoException;
import lombok.RequiredArgsConstructor;
import walaniam.weather.mongo.WeatherDataMongoRepository;
import walaniam.weather.persistence.WeatherData;
import walaniam.weather.persistence.WeatherDataRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static walaniam.weather.common.logging.LoggingUtils.logInfo;
import static walaniam.weather.common.logging.LoggingUtils.logWarn;

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

        logInfo(context, "Observation body data: [%s]", body);

        if (isBlank(body)) {
            return responseOf(request, HttpStatus.BAD_REQUEST, Optional.of("Missing body"));
        } else {
            WeatherDataRepository repository = repositoryProvider.apply(context);
            var data = WeatherData.of(body);
            try {
                repository.save(data);
                return responseOf(request, HttpStatus.OK, Optional.empty());
            } catch (MongoException e) {
                logWarn(context, "Save failed", e);
                return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
            }
        }
    }

    @FunctionName("get-latest-observations-v1")
    public HttpResponseMessage getLatest(
            @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<String> request,
            ExecutionContext context) {

        logInfo(context, "Getting latest observations");

        WeatherDataRepository repository = repositoryProvider.apply(context);
        try {
            List<WeatherDataView> latest = repository.getLatest().stream()
                .map(WeatherDataMapper.INSTANCE::toDataView)
                .collect(Collectors.toList());
            HttpResponseMessage.Builder responseBuilder = responseBuilderOf(request, HttpStatus.OK, Optional.of(latest));
            responseBuilder.header("Content-Type", "application/json");
            return responseBuilder.build();
        } catch (MongoException e) {
            logWarn(context, "read failed", e);
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        }
    }

    private static <T> HttpResponseMessage.Builder responseBuilderOf(HttpRequestMessage<String> request,
                                                      HttpStatus status,
                                                      Optional<T> message) {
        HttpResponseMessage.Builder builder = request.createResponseBuilder(status);
        message.ifPresent(builder::body);
        return builder;
    }

    private static <T> HttpResponseMessage responseOf(HttpRequestMessage<String> request,
                                                      HttpStatus status,
                                                      Optional<T> message) {
        return responseBuilderOf(request, status, message).build();
    }
}
