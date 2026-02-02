package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebKeepAlive {

    public static void start() {
        try {
            // Render sets PORT env var (like 10000, 12345, etc.)
            String portEnv = System.getenv("PORT");
            int port = portEnv != null ? Integer.parseInt(portEnv) : 8080;

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/", exchange -> {
                String response = "Bot is running ✅";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            System.out.println("HTTP health server running on port " + port);
        } catch (IOException e) {
            System.err.println("Failed to start HTTP health server: " + e.getMessage());
        }
    }
}
