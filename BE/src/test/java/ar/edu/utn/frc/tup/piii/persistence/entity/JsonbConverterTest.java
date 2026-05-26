package ar.edu.utn.frc.tup.piii.persistence.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonbConverterTest {

    private JsonbConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonbConverter();
    }

    @Test
    void convertToDatabaseColumn_nullAttribute_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_validAttribute_returnsJsonString() {
        Map<String, Object> attribute = new HashMap<>();
        attribute.put("name", "Venusaur-EX");
        attribute.put("hp", 180);
        attribute.put("rules", List.of("Rule 1", "Rule 2"));

        String json = converter.convertToDatabaseColumn(attribute);
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"Venusaur-EX\""));
        assertTrue(json.contains("\"hp\":180"));
        assertTrue(json.contains("\"rules\":[\"Rule 1\",\"Rule 2\"]"));
    }

    @Test
    void convertToDatabaseColumn_nonSerializableAttribute_throwsIllegalArgumentException() {
        // Create a circular reference or a non-serializable type to trigger JsonProcessingException
        // A self-referencing map:
        Map<String, Object> circularMap = new HashMap<>();
        circularMap.put("self", circularMap);

        assertThrows(IllegalArgumentException.class, () -> {
            converter.convertToDatabaseColumn(circularMap);
        });
    }

    @Test
    void convertToEntityAttribute_nullOrBlankDbData_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(""));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    @SuppressWarnings("unchecked")
    void convertToEntityAttribute_validJsonString_returnsParsedObject() {
        String json = "{\"name\":\"Venusaur-EX\",\"hp\":180,\"rules\":[\"Rule 1\",\"Rule 2\"]}";
        Object result = converter.convertToEntityAttribute(json);

        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("Venusaur-EX", map.get("name"));
        assertEquals(180, map.get("hp"));
        assertTrue(map.get("rules") instanceof List);
        List<String> rules = (List<String>) map.get("rules");
        assertEquals(2, rules.size());
        assertEquals("Rule 1", rules.get(0));
        assertEquals("Rule 2", rules.get(1));
    }

    @Test
    void convertToEntityAttribute_invalidJsonString_throwsIllegalArgumentException() {
        String invalidJson = "{invalid-json-string}";
        assertThrows(IllegalArgumentException.class, () -> {
            converter.convertToEntityAttribute(invalidJson);
        });
    }
}
