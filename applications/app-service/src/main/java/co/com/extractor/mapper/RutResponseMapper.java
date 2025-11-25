package co.com.extractor.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RutResponseMapper {
    co.com.extractor.api.dto.RutResponse toApi(co.com.extractor.domain.model.RutResponse domain);
}

