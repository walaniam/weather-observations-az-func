package walaniam.weather.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import lombok.RequiredArgsConstructor;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static walaniam.weather.common.logging.LoggingUtils.info;

@RequiredArgsConstructor
public class WeatherDataMongoRepository implements WeatherDataRepository {

    private static final String DB_NAME = "weather";
    private static final String COLLECTION_NAME = "observations";

    private final ExecutionContext context;
    private final String connectionString;

    public void save(WeatherData data) {
        info(context, "Saving data: %s", data);
        try (MongoClient client = newMongoClient()) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<WeatherData> collection = db.getCollection(COLLECTION_NAME, WeatherData.class);
            InsertOneResult insertResult = collection.insertOne(data);
            info(context, "Inserted: %s", insertResult);
        }
    }

    private MongoClient newMongoClient() {

        CodecRegistry codecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();

        return MongoClients.create(settings);
    }
}
