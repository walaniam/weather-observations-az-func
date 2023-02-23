package walaniam.weather.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonString;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static walaniam.weather.common.logging.LoggingUtils.logInfo;

public class WeatherDataMongoRepository implements WeatherDataRepository {

    private static final String DB_NAME = "weather";
    private static final String COLLECTION_NAME = "observations";

    private final ExecutionContext context;
    private final MongoClientExecutor<WeatherData> mongoExecutor;

    public WeatherDataMongoRepository(ExecutionContext context, String connectionString) {
        this.context = context;
        this.mongoExecutor = new MongoClientExecutor<>(connectionString, DB_NAME, COLLECTION_NAME, WeatherData.class);
        this.mongoExecutor.execute(collection -> {
            Set<String> indexes = collection.listIndexes()
                    .map(Document::toBsonDocument)
                    .map(it -> it.getString("name"))
                    .map(BsonString::getValue)
                    .into(new HashSet<>());
            if (!indexes.contains("dateTime_-1")) {
                String indexName = collection.createIndex(Indexes.descending("dateTime"));
                logInfo(context, "created index: %s", indexName);
            }
        });
    }

    @Override
    public void save(WeatherData data) {
        logInfo(context, "Saving data: %s", data);
        mongoExecutor.execute(collection -> {
            InsertOneResult insertResult = collection.insertOne(data);
            logInfo(context, "Inserted: %s", insertResult);
        });
    }

    @Override
    public List<WeatherData> getLatest(int limit) {
        logInfo(context, "Getting %s latest observations", limit);
        if (limit < 0 || limit > 1000) {
            throw new IllegalArgumentException("Limit must be in <0, 1000>");
        }
        return mongoExecutor.executeWithResult(collection -> collection
                .find(WeatherData.class)
                .sort(Sorts.descending("dateTime"))
                .limit(limit)
                .into(new ArrayList<>()));
    }
}
