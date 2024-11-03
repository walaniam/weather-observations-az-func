package walaniam.weather.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import walaniam.weather.function.WeatherDataMapper;
import walaniam.weather.function.WeatherDataView;
import walaniam.weather.function.WeatherExtremes;
import walaniam.weather.persistence.WeatherData;
import walaniam.weather.persistence.WeatherDataRepository;

import java.time.LocalDate;
import java.util.*;

import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static walaniam.weather.common.logging.LoggingUtils.logInfo;

public class WeatherDataMongoRepository implements WeatherDataRepository {

    private static final Set<String> REQUIRED_INDEXES = Set.of(
        "dateTime_-1", "outsideTemperature_-1", "outsideTemperature_1", "pressureHpa_-1", "pressureHpa_1"
    );

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
                .into(new TreeSet<>());

            logInfo(context, "Existing indexes=%s", indexes);

            Set<String> missingIndexes = new HashSet<>(REQUIRED_INDEXES);
            missingIndexes.removeAll(indexes);

            missingIndexes.forEach(index -> {
                String name = index.split("_")[0];
                boolean ascending = Integer.parseInt(index.split("_")[1]) > 0;
                if (ascending) {
                    var createdName = collection.createIndex(Indexes.ascending(name));
                    logInfo(context, "created ascending index: %s", createdName);
                } else {
                    var createdName = collection.createIndex(Indexes.descending(name));
                    logInfo(context, "created descending index: %s", createdName);
                }
            });
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

    @Override
    public List<WeatherData> getRange(Integer fromDays, Integer toDays) {
        validateRange(fromDays, toDays);
        var fromDate = LocalDate.now().minusDays(fromDays);
        var toDate = (toDays == null) ? null : LocalDate.now().minusDays(toDays);
        logInfo(context, "Getting in range fromDate=%s, toDate=%s", fromDate, toDate);

        return mongoExecutor.executeWithResult(collection -> {
            Bson filter = (toDate == null)
                ? gte("dateTime", fromDate)
                : Filters.and(gte("dateTime", fromDate), lte("dateTime", toDate));

            return collection
                .find(filter)
                .sort(Sorts.ascending("dateTime"))
                .into(new ArrayList<>());
        });
    }

    @Override
    public WeatherExtremes getExtremes(Integer fromDays, Integer toDays) {

        validateRange(fromDays, toDays);

        var fromDate = LocalDate.now().minusDays(fromDays);
        var toDate = (toDays == null) ? null : LocalDate.now().minusDays(toDays);

        logInfo(context, "Getting extremes fromDate=%s, toDate=%s", fromDate, toDate);

        return mongoExecutor.executeWithResult(collection -> {

            Bson filter = (toDate == null)
                ? gte("dateTime", fromDate)
                : Filters.and(gte("dateTime", fromDate), lte("dateTime", toDate));

            WeatherDataView maxOutside = collection
                .find(filter)
                .sort(Sorts.descending("outsideTemperature"))
                .limit(1)
                .map(WeatherDataMapper.INSTANCE::toDataView)
                .first();

            WeatherDataView minOutSide = collection
                .find(filter)
                .sort(Sorts.ascending("outsideTemperature"))
                .limit(1)
                .map(WeatherDataMapper.INSTANCE::toDataView)
                .first();

            WeatherDataView maxPressure = collection
                .find(filter)
                .sort(Sorts.descending("pressureHpa"))
                .limit(1)
                .map(WeatherDataMapper.INSTANCE::toDataView)
                .first();

            WeatherDataView minPressure = collection
                .find(filter)
                .sort(Sorts.ascending("pressureHpa"))
                .limit(1)
                .map(WeatherDataMapper.INSTANCE::toDataView)
                .first();

            return WeatherExtremes.builder()
                .filter(filter.toString())
                .maxOutsideTemperature(maxOutside)
                .minOutsideTemperature(minOutSide)
                .maxPressure(maxPressure)
                .minPressure(minPressure)
                .build();
        });
    }

    private static void validateRange(Integer fromDays, Integer toDays) {
        if (fromDays == null) {
            throw new IllegalArgumentException("fromDays cannot be null");
        }
        if (fromDays < 1 || (toDays != null && toDays < 1)) {
            throw new IllegalArgumentException("fromDays/toDays must be positive int");
        }
        if (toDays != null && fromDays < toDays) {
            throw new IllegalArgumentException("fromDays must be greater than toDays");
        }
    }
}
