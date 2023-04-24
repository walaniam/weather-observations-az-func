package walaniam.weather.function;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import walaniam.weather.persistence.WeatherData;

@Mapper
public interface WeatherDataMapper {

    WeatherDataMapper INSTANCE = Mappers.getMapper(WeatherDataMapper.class);

    @Mapping(target = "dateTime", source = "dateTime", dateFormat = "yyyyMMdd_HHmmss")
    WeatherDataView toDataView(WeatherData data);
}
