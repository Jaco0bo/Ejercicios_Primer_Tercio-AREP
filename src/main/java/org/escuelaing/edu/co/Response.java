package org.escuelaing.edu.co;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Response {
    private final OutputStream os;
    private final Socket socket;
    private final Map<String,String> headers = new HashMap<>();
    private boolean sent = false;
    private int status = 200;

    public Response(OutputStream os, Socket socket) {
            this.os = os;
            this.socket = socket;
        }

        public void status(int code) {
            this.status = code;
        }

        public void header(String k, String v) {
            headers.put(k,v);
        }

        public void send(String text) throws IOException {
            if (sent) return;
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            if (!headers.containsKey("Content-Type")) {
                headers.put("Content-Type", "text/plain; charset=utf-8");
            }
            writeResponse(status, headers, data);
        }

        public void send(byte[] bytes) throws IOException {
            if (sent) return;
            if (!headers.containsKey("Content-Type")) {
                headers.put("Content-Type", "application/octet-stream");
            }
            writeResponse(status, headers, bytes);
        }

        public boolean isSent() {
            return sent;
        }

        public void sendError(int statusCode, String message) throws IOException {
            if (sent) return;
            this.status = statusCode;
            headers.put("Content-Type", "text/plain; charset=utf-8");
            writeResponse(statusCode, headers, message.getBytes(StandardCharsets.UTF_8));
        }

    // internal helper: writes status line, headers and body, sets sent flag
    private void writeResponse(int code, Map<String,String> headers, byte[] body) throws IOException {
        String reason = reasonPhrase(code);
        // Status line
        os.write(("HTTP/1.1 " + code + " " + reason + "\r\n").getBytes(StandardCharsets.UTF_8));
        // Headers
        if (!headers.containsKey("Content-Length")) {
            os.write(("Content-Length: " + body.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        for (Map.Entry<String,String> e : headers.entrySet()) {
            os.write((e.getKey() + ": " + e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        // Connection: close by default
        if (!headers.containsKey("Connection")) {
            os.write(("Connection: close\r\n").getBytes(StandardCharsets.UTF_8));
        }
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
        // Body
        if (body.length > 0) {
            os.write(body);
        }
        os.flush();
        sent = true;
    }

    private String reasonPhrase(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            default:  return "Status";
        }
    }

    public void setContentType(String contentType) {
        headers.put("Content-Type", contentType);
    }
}
