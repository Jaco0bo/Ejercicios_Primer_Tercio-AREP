package org.escuelaing.edu.co;

import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final String fullPath;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final byte[] body;

    public Request(String method, String path, String fullPath,
                   Map<String, String> queryParams, Map<String, String> headers, byte[] body) {
        this.method = method;
        this.path = path;
        this.fullPath = fullPath;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body == null ? new byte[0] : body;
    }

    // Getters and helpers
    public String getMethod() { return method; }
    public String getPath() { return path; }

    public String getQueryParam(String name, String def) {
        return queryParams.getOrDefault(name, def);
    }

    public String bodyAsString() {
        return new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }
}
