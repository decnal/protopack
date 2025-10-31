package com.example.protocolconverter;

import com.example.protocolconverter.model.ProtocolConverter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProtocolConverterTest {

    private final ProtocolConverter converter = new ProtocolConverter();

    @Test
    public void testConvertToXML() {
        String input = "test";
        String expected = "Converted to XML: " + input;
        assertEquals(expected, converter.convertToXML(input));
    }

    @Test
    public void testConvertToJson() {
        String input = "test";
        String expected = "Converted to JSON: " + input;
        assertEquals(expected, converter.convertToJson(input));
    }

    @Test
    public void testConvertToFlat() {
        String input = "test";
        String expected = "Converted to FLAT: " + input;
        assertEquals(expected, converter.convertToFlat(input));
    }
}
