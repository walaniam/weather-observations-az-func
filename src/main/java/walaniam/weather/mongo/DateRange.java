package walaniam.weather.mongo;

import java.time.LocalDateTime;

public interface DateRange {

    LocalDateTime from();

    LocalDateTime to();
}
