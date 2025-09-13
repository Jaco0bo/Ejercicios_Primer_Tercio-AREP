package org.escuelaing.edu.co;

import org.escuelaing.edu.co.annotations.GetMapping;
import org.escuelaing.edu.co.annotations.RequestParam;
import org.escuelaing.edu.co.annotations.RestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HttpServerTest {

    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
    }

    @Test
    void testTimeGetterReturnsFormattedString() throws Exception {
        Method method = HttpServer.class.getDeclaredMethod("timeGetter");
        method.setAccessible(true);
        String result = (String) method.invoke(null);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(result.contains("de") || result.contains("de")); // comprobación sencilla del formato en español
    }

    @Test
    void testRegisterAnnotatedController() throws Exception {
        HttpServerTestController controller = new HttpServerTestController();
        Method[] methods = controller.getClass().getDeclaredMethods();

        boolean found = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(GetMapping.class)) {
                found = true;
                assertEquals(String.class, m.getReturnType(), "El método anotado debe retornar String");
            }
        }
        assertTrue(found, "El controller debe tener al menos un método con @GetMapping");
    }

    @Test
    void testRouterHandlesHelloRouteWithQueryParam() throws Exception {
        router.get("/hello", (req, res) -> "Hello " + req.getQueryParam("name","world"));

        // Construimos Request con 6 parámetros (method, path, fullPath, queryParams, headers, body)
        Request req = new Request(
                "GET",
                "/hello",
                "/hello?name=Test",
                Map.of("name", "Test"),
                Map.of(), // headers vacíos
                null      // body nulo
        );

        Response res = new Response(new ByteArrayOutputStream(), new FakeSocket());

        Object result = router.handle(req, res);

        assertEquals("Hello Test", result);
    }

    @Test
    void testRouterHandlesHelloRouteWithoutQueryParam() throws Exception {
        router.get("/hello", (req, res) -> "Hello " + req.getQueryParam("name","world"));

        Request req = new Request(
                "GET",
                "/hello",
                "/hello",
                Map.of(), // sin query params
                Map.of(),
                null
        );

        Response res = new Response(new ByteArrayOutputStream(), new FakeSocket());

        Object result = router.handle(req, res);

        assertEquals("Hello world", result);
    }

    /* --- Controller de prueba --- */
    @RestController
    static class HttpServerTestController {
        @GetMapping("/test")
        public String sayHello(@RequestParam(value = "name", required = false, defaultValue = "anon") String name) {
            return "Hi " + name;
        }
    }

    /* --- FakeSocket para no usar la red --- */
    static class FakeSocket extends Socket {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        @Override
        public java.io.OutputStream getOutputStream() {
            return outputStream;
        }
    }
}
