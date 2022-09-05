package walaniam.weather.common.logging;

import com.microsoft.azure.functions.ExecutionContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtils {

    public static void info(ExecutionContext context, String messageTemplate, Object... params) {
        var log = context.getLogger();
        var message = (params != null && params.length > 0)
                ? String.format(messageTemplate, params)
                : messageTemplate;
        message = String.format("[%s] %s", context.getFunctionName(), message);
        log.info(message);
    }
}
