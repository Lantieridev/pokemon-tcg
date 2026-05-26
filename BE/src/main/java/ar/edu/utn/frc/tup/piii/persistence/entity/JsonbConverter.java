package ar.edu.utn.frc.tup.piii.persistence.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonbConverter implements AttributeConverter<Object, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final Object attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting object to JSON string", e);
        }
    }

    @Override
    public Object convertToEntityAttribute(final String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, Object.class);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON string to object", e);
        }
    }
}
