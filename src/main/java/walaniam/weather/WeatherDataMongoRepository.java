package walaniam.weather;

import com.microsoft.azure.functions.ExecutionContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.RequiredArgsConstructor;

import static walaniam.weather.common.logging.LoggingUtils.info;

@RequiredArgsConstructor
public class WeatherDataMongoRepository implements WeatherDataRepository {

    private static final String DB_NAME = "weather";
    private static final String COLLECTION_NAME = "observations";

    private final ExecutionContext context;
    private final String connectionString;

    public void save(WeatherData data) {
        info(context, "Saving data: %s", data);
        try (MongoClient client = MongoClients.create(connectionString)) {
            var db = client.getDatabase(DB_NAME);
            var collection = db.getCollection(COLLECTION_NAME, WeatherData.class);
            var insertResult = collection.insertOne(data);
            info(context, "Inserted: %s", insertResult);
        }
    }

}
