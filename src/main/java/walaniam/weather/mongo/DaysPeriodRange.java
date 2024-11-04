package walaniam.weather.mongo;

import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@ToString
public class DaysPeriodRange implements DateRange {

    private final LocalDateTime dateFrom;
    private final LocalDateTime dateTo;

    public DaysPeriodRange(Integer daysFrom, Integer daysTo) {
        this.dateFrom = utcNowDays().minusDays(daysFrom);
        this.dateTo = (daysTo == null)
            ? null
            : utcNowDays().minusDays(daysTo);
    }

    private static LocalDateTime utcNowDays() {
        return LocalDateTime.now()
            .atOffset(ZoneOffset.UTC)
            .toLocalDateTime()
            .truncatedTo(ChronoUnit.DAYS);
    }

    @Override
    public LocalDateTime from() {
        return dateFrom;
    }

    @Override
    public LocalDateTime to() {
        return dateTo;
    }
}
