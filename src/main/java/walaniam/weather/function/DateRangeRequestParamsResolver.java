package walaniam.weather.function;

import com.microsoft.azure.functions.HttpRequestMessage;
import walaniam.weather.common.time.DateTimeUtils;
import walaniam.weather.mongo.DateRange;
import walaniam.weather.mongo.DaysPeriodRange;

import java.time.LocalDateTime;
import java.util.Optional;

public class DateRangeRequestParamsResolver {

    static DateRange fromRequest(HttpRequestMessage<String> request) {

        LocalDateTime dateFrom = Optional.ofNullable(request.getQueryParameters().get("fromDate"))
            .map(DateTimeUtils::fromUtcString)
            .orElse(null);
        LocalDateTime dateTo = Optional.ofNullable(request.getQueryParameters().get("toDate"))
            .map(DateTimeUtils::fromUtcString)
            .orElse(null);

        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            throw new IllegalArgumentException("dateTo is before dateFrom");
        }

        DateRange dateRange;
        if (dateFrom != null || dateTo != null) {
            dateRange = new DateTimeDateRange(dateFrom, dateTo);
        } else {
            Integer fromDays = Optional.ofNullable(request.getQueryParameters().get("fromDays"))
                .map(Integer::parseInt)
                .orElse(7);
            Integer toDays = Optional.ofNullable(request.getQueryParameters().get("toDays"))
                .map(Integer::parseInt)
                .orElse(null);
            validateRange(fromDays, toDays);
            dateRange = new DaysPeriodRange(fromDays, toDays);
        }

        return dateRange;
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

    private record DateTimeDateRange(LocalDateTime from, LocalDateTime to) implements DateRange {
    }
}
