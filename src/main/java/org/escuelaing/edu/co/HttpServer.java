package org.escuelaing.edu.co;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
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
        try (ServerSocket server = new ServerSocket(36000)) { // Socket del servidor
            System.out.println("Listening on http://localhost:36000/");
            while (true) {
                try (Socket client = server.accept()) { // Socket del cliente
                    handle(client); // Aquí se maneja la conexión del cliente
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handle(Socket client) throws IOException {
        client.setSoTimeout(5000);
        InputStream is = client.getInputStream(); // Entrada del usuario (socket) al servidor
        OutputStream os = client.getOutputStream(); // Salida del servidor (socketServer) al usuario
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)); // Pasamos de bytes a char

        String requestLine = br.readLine();
        if (requestLine == null || requestLine.isEmpty()) return; // Si la petición está vacía o se cerro la conexión, se cierra la petición
        String[] reqParts = requestLine.split(" "); // Separamos por espacios y se espera que este al menos METHOD Y PATH (METHOD PATH HTTP/VERSION)
        if (reqParts.length < 2) return; //Si el mensaje no tiene como mínimo la cabecera (2) y algo adicional (1), se acaba el programa

        String method = reqParts[0]; // GET, POST, PUT ..
        String fullPath = reqParts[1]; // Ejemplo: index.html
        String path = fullPath.split("\\?")[0];

        Map<String,String> headers = new HashMap<>(); // Aquí empezamos a manejar los headers
        String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) { // Si la línea no existe (no hay header) o está vacía (Fin del Header)
            int idx = line.indexOf(':'); // Tomamos como index los ":" que separan nombre de valor
            if (idx > 0) { // si idx existe procedemos a insertar los valores en nuestro HashMap (nombre, valor)
                headers.put(line.substring(0, idx).trim().toLowerCase(), // Tomar Nombre
                        line.substring(idx+1).trim()); // Tomar valor
            }
        }

        // Manejo del endpoint /api/echo
        if (path.equals("/api/echo")) {
            String response;
            int status = 200; // OK

            if ("GET".equals(method)) {
                if (fullPath.contains("?")) {
                    String qs = fullPath.substring(fullPath.indexOf('?') + 1);
                    Map<String,String> params = parseQuery(qs); // Parseamos con parseQuery
                    response = toJson(params); // Se construye un JSON a partir de los String
                } else {
                    response = "{\"msg\":\"OK\"}";
                }

            } else if ("POST".equals(method) || "PUT".equals(method)) {
                // Usamos BufferedReader para leer el body
                String body = readBody(br, headers);
                String ct = headers.getOrDefault("content-type", "").toLowerCase();

                // Si no hay content-type se lanza error 400
                if (ct.isEmpty()) {
                    status = 400;
                    response = "{\"error\":\"missing content-type\"}";
                } else if (ct.contains("application/json")) {
                    // Echo de JSON (si body vacío devolvemos un mensaje)
                    response = body.isEmpty() ? "{\"msg\":\"empty\"}" : body;
                } else if (ct.contains("application/x-www-form-urlencoded")) {
                    // Parsear body como query params
                    Map<String,String> params = parseQuery(body);
                    response = toJson(params);
                } else {
                    // Otros content-types: devolvemos body como string
                    response = "{\"msg\":\"ok\",\"body\":\"" + escapeJson(body) + "\"}";
                }

            } else {
                status = 405; // Method Not Allowed
                response = "{\"error\":\"method not supported\"}";
            }
            sendText(os, status, "application/json; charset=utf-8", response);
            return;
        }

        // Manejo del endpoint /api/time
        if ("/api/time".equals(path)) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy HH:mm:ss", new Locale("es", "ES"));
            String time = now.format(formatter);

            String response = "{\"Fecha\":\"" + escapeJson(time) + "\"}";
            sendText(os, 200, "application/json; charset=utf-8", response);
            return;
        }

        // Manejo del archivo html desde el disco local
        if ("/".equals(path)) path = "/index.html";
        File file = new File(PUBLIC, path);

        String base = PUBLIC.getCanonicalPath(); // Ruta absoluta a la carpeta public
        String candidate = file.getCanonicalPath(); // Ruta absoluta al archivo index.html
        if (!candidate.startsWith(base + File.separator) && !candidate.equals(base)) { // Revisamos que no se intente hacer directory traversal
            sendText(os, 403, "text/plain; charset=utf-8", "Forbidden");
            return;
        }
        if (!file.exists() || file.isDirectory()) {
            sendText(os, 404, "text/html; charset=utf-8", "<h1>404 Not Found</h1>");
            return;
        }
        byte[] data = Files.readAllBytes(file.toPath()); // Leemos el archivo
        String ct = contentType(file.getName()); // Detectamos de qué tipo es html, css, jpg ...
        sendBytes(os, 200, ct, data); // Lo enviamos como respuesta al navegador
    }


    /**
     * Método que se encarga de enviar archivos textuales
     * hacia al navegador (HTML, JSON, ..)
     * @param os
     * @param status
     * @param contentType
     * @param body
     * @throws IOException
     */
    private static void sendText(OutputStream os, int status, String contentType, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8); // Convertimos de texto a bytes
        sendHeaders(os, status, contentType, b.length);
        os.write(b);
        os.flush();
    }

    /**
     * Método que se encarga de enviar archivos binarios
     * como imágenes, videos, etc
     * @param os
     * @param status
     * @param contentType
     * @param data
     * @throws IOException
     */
    private static void sendBytes(OutputStream os, int status, String contentType, byte[] data) throws IOException {
        sendHeaders(os, status, contentType, data.length);
        os.write(data);
        os.flush();
    }

    /**
     * Envía al cliente los encabezados HTTP de la respuesta.
     * Incluye:
     * - Línea de estado (ejemplo: "HTTP/1.1 200 OK")
     * - Content-Type (tipo MIME del recurso)
     * - Content-Length (tamaño del cuerpo en bytes)
     * - Connection: close (cerrar conexión tras la respuesta)
     *
     * @param os         canal de salida hacia el cliente
     * @param status     código de estado HTTP (200, 404, 403, etc.)
     * @param contentType tipo MIME del contenido a enviar (ejemplo: text/html, application/json)
     * @param length     tamaño del cuerpo de la respuesta en bytes
     * @throws IOException si ocurre un error al escribir en el stream
     */
    private static void sendHeaders(OutputStream os, int status, String contentType, int length) throws IOException {
        String reason = (status==200) ? "OK" : (status==404 ? "Not Found" : (status==403 ? "Forbidden" : "OK"));
        os.write(("HTTP/1.1 " + status + " " + reason + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Length: " + length + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Connection: close\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("\r\n").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Determina el tipo MIME de un archivo a partir de su extensión.
     * Soporta extensiones comunes como .html, .css, .js, .png, .jpg, .gif.
     * Si no reconoce la extensión, devuelve "application/octet-stream"
     * (tipo genérico para binarios).
     *
     * @param name nombre del archivo
     * @return cadena con el tipo MIME correspondiente
     */
    private static String contentType(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    /**
     * Lee el cuerpo (body) de una petición HTTP a partir del encabezado Content-Length.
     * El método busca en los encabezados el valor de "content-length". Si no está presente
     * o no es un número válido, retorna una cadena vacía.
     * Si el valor es válido, se leen exactamente esa cantidad de caracteres desde el
     * BufferedReader proporcionado y se construye un String con el contenido.
     *
     * @param br      BufferedReader conectado al input stream de la petición.
     * @param headers Mapa con los encabezados HTTP, usado para obtener el Content-Length.
     * @return El cuerpo de la petición como cadena de texto, o vacío si no hay cuerpo.
     * @throws IOException Sí ocurre un error al leer del stream.
     */
    private static String readBody(BufferedReader br, Map<String,String> headers) throws IOException {
        String cl = headers.get("content-length"); // Busca el valor de la longitud del body
        if (cl == null) return ""; // Si no existe, no hay body
        int length;
        try {
            length = Integer.parseInt(cl.trim());
        } catch (NumberFormatException e) {
            return ""; // Si no regresa un numero valido, devolvemos una cadena vaciá
        }
        char[] buf = new char[length];
        int read = 0;
        while (read < length) { // Lee hasta que se llegue a length o no hallan mas datos
            int r = br.read(buf, read, length - read);
            if (r == -1) break;
            read += r;
        }
        return new String(buf, 0, read);
    }

    /**
     * Convierte una query string (ejemplo: "name=Juan&age=25") en un mapa clave-valor.
     * Divide la cadena por el carácter '&' para separar pares, y luego cada par
     * por '=' para obtener clave y valor. Se aplica URL decoding en UTF-8 a ambos.
     * Si no hay valor, se guarda como cadena vacía.
     *
     * @param qs Cadena con la query string (ejemplo: "a=1&b=2").
     * @return Un mapa con las claves y valores parseados. Retorna vacío si la cadena es nula o vacía.
     */
    private static Map<String,String> parseQuery(String qs) {
        Map<String,String> map = new LinkedHashMap<>();
        if (qs == null || qs.isEmpty()) return map;
        String[] pairs = qs.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String val = idx >= 0 ? pair.substring(idx + 1) : "";
            try {
                key = java.net.URLDecoder.decode(key, "UTF-8");
                val = java.net.URLDecoder.decode(val, "UTF-8");
            } catch (Exception e) {
                // ignore, usar raw si falla
            }
            map.put(key, val);
        }
        return map;
    }

    /**
     * Convierte un mapa clave-valor en una representación JSON válida.
     * Cada clave y valor se escapan usando {@link #escapeJson(String)} para garantizar
     * que el resultado sea un JSON correcto.
     *
     * @param map Mapa con las claves y valores a convertir.
     * @return Cadena en formato JSON representando el mapa.
     */
    private static String toJson(Map<String,String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String,String> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            sb.append("\"").append(escapeJson(e.getValue())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escapa caracteres especiales de una cadena para que sea segura en JSON.
     * Sustituye comillas, barras invertidas, saltos de línea, tabulaciones, etc.
     * Además, convierte caracteres no imprimibles o fuera de ASCII en su
     * representación Unicode.
     *
     * @param s Cadena de entrada a escapar.
     * @return Cadena escapada, lista para usarse en un JSON.
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7E) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

