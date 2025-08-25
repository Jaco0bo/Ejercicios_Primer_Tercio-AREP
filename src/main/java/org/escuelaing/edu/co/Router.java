package org.escuelaing.edu.co;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class Router {
    private File staticRoot = null;
    private final Map<String, BiFunction<Request, Response, Object>> routes = new ConcurrentHashMap<>();

    public Router() {}

    // ---- registration helpers ----
    public void get(String path, BiFunction<Request, Response, Object> handler) {
        put("GET", path, handler);
    }

    public void post(String path, BiFunction<Request, Response, Object> handler) {
        put("POST", path, handler);
    }

    public void put(String path, BiFunction<Request, Response, Object> handler) {
        put("PUT", path, handler);
    }

    public void delete(String path, BiFunction<Request, Response, Object> handler) {
        put("DELETE", path, handler);
    }

    private void put(String method, String path, BiFunction<Request, Response, Object> handler) {
        String k = key(method, normalize(path));
        routes.put(k, handler);
    }

    private String key(String method, String path) {
        return method.toUpperCase() + ":" + path;
    }

    private String normalize(String path) {
        if (path == null || path.isEmpty()) return "/";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    public void staticFiles(String directory) {
        this.staticRoot = new File(directory);
    }

    // ---- dispatcher ----
    public Object handle(Request req, Response res) throws IOException {
        String method = req.getMethod() == null ? "GET" : req.getMethod().toUpperCase();
        String rawPath = req.getPath() == null ? "/" : req.getPath();
        String path = normalize(rawPath);

        // try route handler
        BiFunction<Request, Response, Object> handler = routes.get(key(method, path));
        if (handler != null) {
            try {
                return handler.apply(req, res);
            } catch (Exception e) {
                if (!res.isSent()) {
                    try { res.sendError(500, "Internal Server Error"); } catch (IOException ignored) {}
                }
                return null;
            }
        }

        // fallback: static files
        if (staticRoot != null) {
            try {
                // decode URL encoded parts (eg. %20)
                String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
                if ("/".equals(decoded)) decoded = "/index.html";

                // make relative path to avoid absolute child issues
                String rel = decoded.startsWith("/") ? decoded.substring(1) : decoded;
                File candidate = new File(staticRoot, rel);

                String base = staticRoot.getCanonicalPath();
                String candidatePath = candidate.getCanonicalPath();

                if (!candidatePath.startsWith(base + File.separator) && !candidatePath.equals(base)) {
                    res.sendError(403, "Forbidden");
                    return null;
                }
                if (!candidate.exists() || candidate.isDirectory()) {
                    res.sendError(404, "Not Found");
                    return null;
                }

                byte[] data = java.nio.file.Files.readAllBytes(candidate.toPath());
                String mime = contentType(candidate.getName());
                res.header("Content-Type", mime);
                res.send(data);
                return null;
            } catch (Exception e) {
                if (!res.isSent()) {
                    try { res.sendError(500, "Internal Server Error"); } catch (IOException ignored) {}
                }
                return null;
            }
        }

        // nothing handled it -> 404
        if (!res.isSent()) {
            res.sendError(404, "Not Found");
        }
        return null;
    }

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
}

