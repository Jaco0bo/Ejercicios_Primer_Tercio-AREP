package org.escuelaing.edu.co;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {

    public static Request parse(Socket client, int timeoutMillis) throws IOException, ParseException {
        try {
            client.setSoTimeout(timeoutMillis);
            InputStream is = client.getInputStream(); // Entrada del usuario (socket) al servidor
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            String requestLine = br.readLine();
            if (requestLine == null || requestLine.isEmpty()) return null; // Si la petición está vacía o se cerro la conexión, se cierra la petición
            String[] reqParts = requestLine.split(" "); // Separamos por espacios y se espera que este al menos METHOD Y PATH (METHOD PATH HTTP/VERSION)
            if (reqParts.length < 2) return null; //Si el mensaje no tiene como mínimo la cabecera (2) y algo adicional (1), se acaba el programa

            String method = reqParts[0]; // GET, POST, PUT ..
            String fullPath = reqParts[1]; // Ejemplo: index.html
            String path = fullPath.split("\\?")[0];
            String queryString = fullPath.contains("?") ? fullPath.substring(fullPath.indexOf('?') + 1) : "";
            Map<String,String> queryParams = parseQuery(queryString);

            // Headers
            Map<String,String> headers = new HashMap<>();
            String line;
            while ((line = br.readLine()) != null && !line.isEmpty()) { // Si la línea no existe (no hay header) o está vacía (Fin del Header)
                int idx = line.indexOf(':'); // Tomamos como index los ":" que separan nombre de valor
                if (idx > 0) { // si idx existe procedemos a insertar los valores en nuestro HashMap (nombre, valor)
                    String name = line.substring(0, idx).trim().toLowerCase();
                    String value = line.substring(idx + 1).trim();
                    headers.put(name, value);
                }
            }

            // Body (Solo si tiene Content-Length)
            byte[] bodyBytes = new byte[0];
            String cl = headers.get("content-length");
            if (cl != null) {
                try {
                    int length = Integer.parseInt(cl.trim());
                    if (length > 0) {
                        char[] buf = new char[length];
                        int totalRead = 0;
                        while (totalRead < length) {
                            int r = br.read(buf, totalRead, length - totalRead);
                            if (r == -1) break;
                            totalRead += r;
                        }
                        String bodyText = new String(buf, 0, totalRead);
                        bodyBytes = bodyText.getBytes(StandardCharsets.UTF_8);
                    }
                } catch (NumberFormatException nfe) {
                    throw new ParseException("Invalid Content-Length: " + cl, 0);
                }
            }

            return new Request(method, path, fullPath, queryParams, headers, bodyBytes);
        } catch (SocketTimeoutException ste) {
            System.out.println("Read timed out from " + client.getRemoteSocketAddress());
            return null;
        } catch (IOException ioe) {
            throw new ParseException("I/O reading request: " + ioe.getMessage(), 0);
        }
    }

    private static Map<String,String> parseQuery(String queryString) {
        Map<String,String> queryParams = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return queryParams;
        String[] pairs = queryString.split("&");
        for (String p : pairs) {
            String[] kv = p.split("=", 2);
            String key = urlDecode(kv[0]);
            String val = kv.length > 1 ? urlDecode(kv[1]) : "";
            queryParams.put(key, val);
        }
        return queryParams;
    }

    private static byte[] readBody(InputStream is, int length) throws IOException {
        byte[] body = new byte[length];
        int read = 0;
        while (read < length) {
            int r = is.read(body, read, length - read);
            if (r == -1) break;
            read += r;
        }
        return Arrays.copyOf(body, read);
    }

    private static String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, "UTF-8"); } catch(Exception e) { return s; }
    }
}



