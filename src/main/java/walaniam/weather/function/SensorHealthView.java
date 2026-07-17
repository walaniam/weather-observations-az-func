package walaniam.weather.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SensorHealthView {

    private boolean stale;
    private String lastSeen;
    private Long minutesSinceLast;
    private int staleMinutes;
}
