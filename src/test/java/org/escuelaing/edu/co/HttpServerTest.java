package org.escuelaing.edu.co;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpServerTest {

    @Test
    void testParseQuerySimple() {
        Map<String, String> result = callParseQuery("name=Juan&age=25");
        assertEquals("Juan", result.get("name"));
        assertEquals("25", result.get("age"));
    }

    @Test
    void testParseQueryWithEncoding() {
        Map<String, String> result = callParseQuery("city=Bogot%C3%A1");
        assertEquals("Bogotá", result.get("city"));
    }

    @Test
    void testToJson() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("name", "Juan");
        input.put("age", "25");
        String json = callToJson(input);
        assertEquals("{\"name\":\"Juan\",\"age\":\"25\"}", json);
    }

    @Test
    void testEscapeJsonSpecialChars() {
        String input = "Hola \"Mundo\"\n";
        String escaped = callEscapeJson(input);
        assertEquals("Hola \\\"Mundo\\\"\\n", escaped);
    }

    @Test
    void testContentTypeHtml() {
        assertEquals("text/html; charset=utf-8", callContentType("index.html"));
    }

    @Test
    void testContentTypeUnknown() {
        assertEquals("application/octet-stream", callContentType("archivo.xyz"));
    }

    @Test
    void testReadBody() throws Exception {
        String body = "name=Juan";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-length", String.valueOf(body.length()));

        BufferedReader br = new BufferedReader(new StringReader(body));
        String result = callReadBody(br, headers);
        assertEquals("name=Juan", result);
    }

    // --- Métodos helpers para acceder a los métodos privados ---
    private Map<String, String> callParseQuery(String qs) {
        try {
            var m = HttpServer.class.getDeclaredMethod("parseQuery", String.class);
            m.setAccessible(true);
            return (Map<String, String>) m.invoke(null, qs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callToJson(Map<String, String> map) {
        try {
            var m = HttpServer.class.getDeclaredMethod("toJson", Map.class);
            m.setAccessible(true);
            return (String) m.invoke(null, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callEscapeJson(String s) {
        try {
            var m = HttpServer.class.getDeclaredMethod("escapeJson", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callContentType(String name) {
        try {
            var m = HttpServer.class.getDeclaredMethod("contentType", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callReadBody(BufferedReader br, Map<String, String> headers) {
        try {
            var m = HttpServer.class.getDeclaredMethod("readBody", BufferedReader.class, Map.class);
            m.setAccessible(true);
            return (String) m.invoke(null, br, headers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

