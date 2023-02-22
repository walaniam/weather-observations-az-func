package walaniam.weather.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static walaniam.weather.common.logging.LoggingUtils.logInfo;

public class WeatherDataMongoRepository implements WeatherDataRepository {

    private static final String DB_NAME = "weather";
    private static final String COLLECTION_NAME = "observations";

    private final ExecutionContext context;
    private final String connectionString;

    public WeatherDataMongoRepository(ExecutionContext context, String connectionString) {
        this.context = context;
        this.connectionString = connectionString;
        try (MongoClient client = newMongoClient()) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<WeatherData> collection = db.getCollection(COLLECTION_NAME, WeatherData.class);
            Set<String> indexes = collection.listIndexes()
                    .map(Document::toBsonDocument)
                    .map(it -> it.getString("name"))
                    .map(BsonString::getValue)
                    .into(new HashSet<>());
            if (!indexes.contains("dateTime_-1")) {
                String indexName = collection.createIndex(Indexes.descending("dateTime"));
                logInfo(context, "created index: %s", indexName);
            }
        }
    }

    @Override
    public void save(WeatherData data) {
        logInfo(context, "Saving data: %s", data);
        try (MongoClient client = newMongoClient()) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<WeatherData> collection = db.getCollection(COLLECTION_NAME, WeatherData.class);
            InsertOneResult insertResult = collection.insertOne(data);
            logInfo(context, "Inserted: %s", insertResult);
        }
    }

    @Override
    public List<WeatherData> getLatest(int limit) {
        logInfo(context, "Getting %s latest observations", limit);
        if (limit < 0 || limit > 1000) {
            throw new IllegalArgumentException("Limit must be in <0, 1000>");
        }
        try (MongoClient client = newMongoClient()) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<WeatherData> collection = db.getCollection(COLLECTION_NAME, WeatherData.class);
            return collection
                    .find(WeatherData.class)
                    .sort(Sorts.descending("dateTime"))
                    .limit(10)
                    .into(new ArrayList<>());
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
