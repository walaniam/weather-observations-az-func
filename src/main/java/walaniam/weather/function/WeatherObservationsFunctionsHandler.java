package walaniam.weather.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.mongodb.MongoException;
import lombok.RequiredArgsConstructor;
import walaniam.weather.common.time.DateTimeUtils;
import walaniam.weather.mongo.DateRange;
import walaniam.weather.mongo.WeatherDataMongoRepository;
import walaniam.weather.persistence.WeatherData;
import walaniam.weather.persistence.WeatherDataRepository;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static walaniam.weather.common.logging.LoggingUtils.logInfo;
import static walaniam.weather.common.logging.LoggingUtils.logWarn;

@RequiredArgsConstructor
public class WeatherObservationsFunctionsHandler {

    private final Function<ExecutionContext, WeatherDataRepository> repositoryProvider;

    @SuppressWarnings("unused")
    public WeatherObservationsFunctionsHandler() {
        this(Optional.ofNullable(System.getenv("CosmosDBConnectionString"))
            .orElseGet(() -> {
                String localEnvMongo = "mongodb://mongo:mongo@localhost/weather_local:27017?ssl=false&authSource=admin";
                System.out.println("Using LOCAL env connection: " + localEnvMongo);
                System.out.println();
                return localEnvMongo;
            }));
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
                .toList();
            HttpResponseMessage.Builder responseBuilder = responseBuilderOf(request, HttpStatus.OK, Optional.of(latest));
            responseBuilder.header("Content-Type", "application/json");
            return responseBuilder.build();
        } catch (MongoException e) {
            logWarn(context, "read failed", e);
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        }
    }

    @FunctionName("get-latest-observation-v1")
    public HttpResponseMessage getSingleLatest(
        @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<String> request,
        ExecutionContext context) {

        logInfo(context, "Getting latest single observation");

        WeatherDataRepository repository = repositoryProvider.apply(context);
        try {
            WeatherDataView latest = repository.getLatest(1).stream()
                .map(WeatherDataMapper.INSTANCE::toDataView)
                .findFirst()
                .orElseThrow();
            HttpResponseMessage.Builder responseBuilder = responseBuilderOf(request, HttpStatus.OK, Optional.of(latest));
            responseBuilder.header("Content-Type", "application/json");
            return responseBuilder.build();
        } catch (NoSuchElementException e) {
            return responseOf(request, HttpStatus.NOT_FOUND, Optional.empty());
        } catch (MongoException e) {
            logWarn(context, "read failed", e);
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        }
    }

    @FunctionName("get-chart-v1")
    public HttpResponseMessage getChart(
        @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<String> request,
        ExecutionContext context) {

        DateRange dateRange = DateRangeRequestParamsResolver.fromRequest(request);

        logInfo(context, "Getting extremes of in range %s", dateRange);

        WeatherDataRepository repository = repositoryProvider.apply(context);
        try {
            List<WeatherData> observations = repository.getRange(dateRange);
            String htmlPage = HtmlGenerator.generateHtmlWithImage(observations);

            HttpResponseMessage.Builder builder = responseBuilderOf(request, HttpStatus.OK, Optional.of(htmlPage));
            builder.header("Content-Type", "text/html");
            return builder.build();

        } catch (NoSuchElementException e) {
            return responseOf(request, HttpStatus.NOT_FOUND, Optional.empty());
        } catch (MongoException e) {
            logWarn(context, "read failed", e);
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        } catch (IOException e) {
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        }
    }

    @FunctionName("get-extremes-v1")
    public HttpResponseMessage getExtremes(
        @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<String> request,
        ExecutionContext context) {

        DateRange dateRange = DateRangeRequestParamsResolver.fromRequest(request);

        logInfo(context, "Getting extremes in range=%s", dateRange);

        WeatherDataRepository repository = repositoryProvider.apply(context);
        try {
            WeatherExtremes extremes = repository.getExtremes(dateRange);
            HttpResponseMessage.Builder builder = responseBuilderOf(request, HttpStatus.OK, Optional.of(extremes));
            builder.header("Content-Type", "application/json");
            return builder.build();
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
