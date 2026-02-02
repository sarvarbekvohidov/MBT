package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SelfPinger {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private static final Random random = new Random();

    public static void start(String url) {
        Runnable pingTask = new Runnable() {
            @Override
            public void run() {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();

                    HttpResponse<String> response =
                            client.send(request, HttpResponse.BodyHandlers.ofString());

                    System.out.println("✅ Self-ping success: " + response.statusCode());
                } catch (Exception e) {
                    System.err.println("❌ Self-ping failed: " + e.getMessage());
                }

                // Pick next delay: 1–10 minutes (random every time)
                int nextDelay = 1 + random.nextInt(10);
                System.out.println("⏳ Next ping in " + nextDelay + " minute(s)");

                scheduler.schedule(this, nextDelay, TimeUnit.MINUTES);
            }
        };

        // Start immediately
        scheduler.schedule(pingTask, 0, TimeUnit.MINUTES);
    }
}
