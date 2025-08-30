package org.escuelaing.edu.co;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import org.escuelaing.edu.co.annotations.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpServer {
    /**
     * Dirección donde se almacena toda la página, imágenes. (recursos estáticos).
     */
    private static final File PUBLIC = new File("src/main/resources/public");
    private static Object controllerInstance = null;
    private static final Map<String, Method> routeHandlers = new HashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java -cp target/classes co.edu.escuelaing.reflexionlab.HttpServer <fully.qualified.ControllerClass>");
            return;
        }

        String controllerClass = args[0];
        loadRoutes(controllerClass);

        if (!PUBLIC.exists()) PUBLIC.mkdirs();
        Scanner sc = new Scanner(System.in);
        System.out.print("Por favor ingrese puerto: ");
        int puerto = sc.nextInt();
        try (ServerSocket server = new ServerSocket(puerto)) { // Socket del servidor
            System.out.println("Listening on http://localhost:"+puerto+"/");
            Router router = new Router();
            for (Map.Entry<String, Method> entry : routeHandlers.entrySet()) {
                final String path = entry.getKey();
                final Method m = entry.getValue();
                // make accessible if necessary (but prefer public methods)
                m.setAccessible(true);

                router.get(path, (req, res) -> {
                    try {
                        // invoke no-arg method (we already validated signature)
                        String result = (String) m.invoke(controllerInstance);
                        // default content type for controller strings
                        res.setContentType("text/html; charset=utf-8");
                        return result == null ? "" : result;
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        try {
                            res.sendError(500, "Not Valid");
                            res.setContentType("text/plain; charset=utf-8");
                        } catch (Throwable ignore) {}
                        return "Internal Server Error";
                    }
                });

                System.out.println("Mounted annotated route: GET " + path + " -> " + m.getName());
            }
            router.staticFiles("src/main/resources/public");
            router.get("/hello", (req, res) -> "Hello " + req.getQueryParam("name","world"));
            router.post("/api/echo", (req, res) -> req.bodyAsString());
            router.get("/api/time", (req, res) -> {
                res.setContentType("application/json");
                return "{ \"time\": \"" + timeGetter() + "\" }";
            });


            while (true) {
                try (Socket client = server.accept()) { // Socket del cliente
                    handle(client, router); // Aquí se maneja la conexión del cliente
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void loadRoutes(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            if (!clazz.isAnnotationPresent(RestController.class)) {
                System.err.println("Class " + className + " is not annotated with @RestController");
                return;
            }

            // instantiate controller (requires public no-arg constructor)
            controllerInstance = clazz.getDeclaredConstructor().newInstance();

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(GetMapping.class)) continue;

                // Validate signature: public, no-args, returns String
                if (!Modifier.isPublic(method.getModifiers())) {
                    System.err.println("Skipping " + method.getName() + ": method not public");
                    continue;
                }
                if (method.getParameterCount() != 0) {
                    System.err.println("Skipping " + method.getName() + ": only no-arg handlers supported");
                    continue;
                }
                if (!method.getReturnType().equals(String.class)) {
                    System.err.println("Skipping " + method.getName() + ": return type must be String");
                    continue;
                }

                GetMapping gm = method.getAnnotation(GetMapping.class);
                String path = gm.value();
                routeHandlers.put(path, method);
                System.out.println("Registered route: GET " + path + " -> " + method.getName());
            }
        } catch (ClassNotFoundException cnf) {
            System.err.println("Controller class not found: " + className);
            cnf.printStackTrace();
        } catch (NoSuchMethodException nsme) {
            System.err.println("No-arg constructor missing in " + className);
            nsme.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error loading controller " + className);
            e.printStackTrace();
        }
    }

    private static void handle(Socket client, Router router) {
        try {
            Request req = RequestParser.parse(client, 5000);
            if (req == null) {
                return;
            }

            Response res = new Response(client.getOutputStream(), client);

            try {
                Object result = router.handle(req, res);

                // If the handler already sent the response, do nothing
                if (res.isSent()) return;

                if (result == null) {
                    // No content
                    res.status(204);
                    res.send(new byte[0]);
                } else if (result instanceof String) {
                    if (!res.isSent()) {
                        res.header("Content-Type", "text/plain; charset=utf-8");
                        res.send(((String) result));
                    }
                } else if (result instanceof byte[]) {
                    if (!res.isSent()) {
                        res.send((byte[]) result);
                    }
                } else {
                    // fallback: toString
                    if (!res.isSent()) {
                        res.header("Content-Type", "text/plain; charset=utf-8");
                        res.send(result.toString());
                    }
                }
            } catch (Exception handlerEx) {
                try {
                    if (!res.isSent()) res.sendError(500, "Internal Server Error");
                } catch (IOException ignore) {}
            }
        } catch (ParseException pe) {
            try {
                Response res = new Response(client.getOutputStream(), client);
                res.sendError(400, "Bad Request");
            } catch (IOException ignore) {}
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private static String timeGetter() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy HH:mm:ss", new Locale("es", "ES"));
        return now.format(formatter);
    }

}

