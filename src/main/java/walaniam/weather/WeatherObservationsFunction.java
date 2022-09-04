package walaniam.weather;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class WeatherObservationsFunction {

    @FunctionName("observations-v1")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = HttpMethod.POST, authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<String> request,
            @CosmosDBOutput(
                    name = "databaseOutput",
                    databaseName = "weather",
                    collectionName = "observations",
                    connectionStringSetting = "CosmosDBConnectionString"
            )
            OutputBinding<WeatherData> document,
            ExecutionContext context) {

        final var log = context.getLogger();

        var body = request.getBody();

        log.info("Observation body data: " + body);

        if (isBlank(body)) {
            return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .build();
        } else {
            var data = WeatherData.of(body);
            log.info("Saving data: " + data);
            document.setValue(data);
            return request
                    .createResponseBuilder(HttpStatus.OK)
                    .build();
        }
    }
}
