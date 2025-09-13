package org.escuelaing.edu.co;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.net.*;
import org.escuelaing.edu.co.annotations.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpServer {
    private static final File PUBLIC = new File("src/main/resources/public");
    private static Object controllerInstance = null;
    private static final Map<String, Method> routeHandlers = new HashMap<>();
    private static final Map<Method, Object> methodToInstance = new HashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            for (String s : args) {
                if (s.contains(".")) { // heuristic: looks like fqcn
                    try {
                        Class<?> c = Class.forName(s);
                        loadRoutesFromClass(c);
                    } catch (ClassNotFoundException e) {
                        System.err.println("Explicit controller class not found: " + s);
                    }
                }
            }
        }

        List<String> discovered = discoverControllersOnClasspath();
        for (String fqcn : discovered) {
            try {
                Class<?> c = Class.forName(fqcn);
                loadRoutesFromClass(c);
            } catch (Throwable ignore) {}
        }

        System.out.println("Total annotated routes registered: " + routeHandlers.size());

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
                final Object instance = methodToInstance.get(m);
                m.setAccessible(true);

                router.get(path, (req, res) -> {
                    try {
                        // prepare args for the method based on @RequestParam
                        Parameter[] params = m.getParameters();
                        Object[] argsForInvoke = new Object[params.length];

                        for (int i = 0; i < params.length; i++) {
                            Parameter p = params[i];
                            org.escuelaing.edu.co.annotations.RequestParam rp = p.getAnnotation(org.escuelaing.edu.co.annotations.RequestParam.class);
                            String name = rp.value();
                            String defaultValue = rp.defaultValue();
                            boolean required = rp.required();

                            // assume your Request object has getQueryParam(name, defaultValue)
                            String value = req.getQueryParam(name, null);
                            if (value == null) {
                                if (!defaultValue.isEmpty()) value = defaultValue;
                                else if (!required) value = null;
                                else {
                                    res.sendError(400, "No value for parameter " + name);
                                    res.setContentType("text/plain; charset=utf-8");
                                    return "Missing required query parameter: " + name;
                                }
                            }
                            argsForInvoke[i] = value;
                        }

                        String result = (String) m.invoke(instance, argsForInvoke);
                        res.setContentType("text/html; charset=utf-8");
                        return result == null ? "" : result;
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        try { res.sendError(500, "Unexpected Error"); res.setContentType("text/plain; charset=utf-8"); } catch (Throwable ignore) {}
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

    private static List<String> discoverControllersOnClasspath() {
        List<String> found = new ArrayList<>();
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(System.getProperty("path.separator"));
        for (String entry : entries) {
            File f = new File(entry);
            if (f.exists() && f.isDirectory()) {
                Path root = f.toPath();
                try {
                    Files.walkFileTree(root, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (file.toString().endsWith(".class")) {
                                // compute fqcn relative to root
                                Path rel = root.relativize(file);
                                String q = rel.toString().replace(File.separatorChar, '.');
                                if (q.endsWith(".class")) q = q.substring(0, q.length() - 6);
                                try {
                                    Class<?> c = Class.forName(q);
                                    if (c.isAnnotationPresent(org.escuelaing.edu.co.annotations.RestController.class)) {
                                        found.add(q);
                                    }
                                } catch (Throwable ignore) {
                                    // skip classes we cannot load
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                }
            }
        }
        return found;
    }

    private static void loadRoutesFromClass(Class<?> clazz) {
        try {
            if (!clazz.isAnnotationPresent(org.escuelaing.edu.co.annotations.RestController.class)) return;

            Object instance = clazz.getDeclaredConstructor().newInstance();
            // store controller instance so we can invoke later (keyed by class for simplicity)
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(org.escuelaing.edu.co.annotations.GetMapping.class)) continue;

                // Must return String
                if (!method.getReturnType().equals(String.class)) {
                    System.err.println("Skipping " + clazz.getName() + "." + method.getName() + ": return type must be String");
                    continue;
                }

                boolean ok = true;
                for (Parameter p : method.getParameters()) {
                    if (!p.isAnnotationPresent(org.escuelaing.edu.co.annotations.RequestParam.class)) {
                        ok = false;
                        System.err.println("Skipping " + clazz.getName() + "." + method.getName() + ": all parameters must be annotated with @RequestParam");
                        break;
                    }

                    if (!p.getType().equals(String.class)) {
                        ok = false;
                        System.err.println("Skipping " + clazz.getName() + "." + method.getName() + ": parameter types other than String are not supported yet");
                        break;
                    }
                }
                if (!ok) continue;

                org.escuelaing.edu.co.annotations.GetMapping gm = method.getAnnotation(org.escuelaing.edu.co.annotations.GetMapping.class);
                String path = gm.value();
                routeHandlers.put(path, method);
                methodToInstance.put(method, instance);
                System.out.println("Registered route: GET " + path + " -> " + clazz.getName() + "." + method.getName());
            }
        } catch (Exception e) {
            System.err.println("Error loading routes from class " + clazz.getName());
            e.printStackTrace();
        }
    }
}

