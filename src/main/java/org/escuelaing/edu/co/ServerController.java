package org.escuelaing.edu.co;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class ServerController {
    private final Router router;
    private ServerSocket server;
    private Thread acceptThread;
    private ThreadPoolExecutor pool;
    private volatile boolean running = false;

    public ServerController(Router router) {
        this.router = router;
    }

    public void start(int port) throws IOException {
        if (running) return;
        server = new ServerSocket(port);

        int core = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
        int max = core * 2;
        pool = new ThreadPoolExecutor(
                core,
                max,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        running = true;
        acceptThread = new Thread(() -> {
            System.out.println("Listening on http://localhost:" + port + "/");
            while (running && !server.isClosed()) {
                try {
                    final Socket client = server.accept();
                    pool.submit(() -> {
                        try {
                            HttpServer.handle(client, router);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        } finally {
                            try { client.close(); } catch (IOException ignored) {}
                        }
                    });
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }, "http-acceptor-thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void stop() {
        if (!running) return;
        running = false;

        try { if (server != null && !server.isClosed()) server.close(); } catch (IOException ignored) {}

        try {
            if (acceptThread != null) acceptThread.join(2000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ex) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Server stopped.");
    }
}
