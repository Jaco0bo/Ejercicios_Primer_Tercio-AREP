package org.escuelaing.edu.co;

import org.junit.jupiter.api.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {

    private static Thread serverThread;
    private static final int PORT = 9090;
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeAll
    static void startServer() throws Exception {
        serverThread = new Thread(() -> {
            try {
                HttpServer.main(new String[]{String.valueOf(PORT)});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();


        Thread.sleep(2000);
    }

    @AfterAll
    static void stopServer() throws Exception {
        serverThread.interrupt();
    }

    @Test
    void testConcurrentRequestsToGreeting() throws Exception {
        int numClients = 30; // número de peticiones concurrentes
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            futures.add(executor.submit(() -> sendRequest("/greeting?name=test-" + clientId)));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Timeout waiting for tasks");

        for (int i = 0; i < futures.size(); i++) {
            String response = futures.get(i).get();
            assertNotNull(response, "Response should not be null for client " + i);
            assertTrue(response.contains("Hello"), "Response should contain 'Hello' for client " + i);
        }
    }

    private String sendRequest(String endpoint) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("HTTP error: " + status);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
