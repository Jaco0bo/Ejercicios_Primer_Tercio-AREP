package org.escuelaing.edu.co;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpServer {
    /**
     * Dirección donde se almacena toda la página, imágenes. (recursos estáticos).
     */
    private static final File PUBLIC = new File("src/main/resources/public");

    public static void main(String[] args) throws IOException {
        if (!PUBLIC.exists()) PUBLIC.mkdirs();
        Scanner sc = new Scanner(System.in);
        System.out.print("Por favor ingrese puerto: ");
        int puerto = sc.nextInt();
        try (ServerSocket server = new ServerSocket(puerto)) { // Socket del servidor
            System.out.println("Listening on http://localhost:"+puerto+"/");
            Router router = new Router();
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

