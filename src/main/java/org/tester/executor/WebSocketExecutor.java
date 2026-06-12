package org.tester.executor;

import org.tester.metrics.RequestMetric;
import org.tester.model.ApiStep;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/** JDK {@link java.net.http.WebSocket} executor for persona WebSocket steps. */
public class WebSocketExecutor {

    private final HttpClient client = HttpClient.newHttpClient();

    public CompletableFuture<RequestMetric> executeAsync(
            String userId,
            String personaName,
            ApiStep step
    ) {
        long startTime = System.currentTimeMillis();
        WebSocketListener listener = new WebSocketListener();
        int waitTime = step.waitTimeMs == null ? 3000 : step.waitTimeMs;

        return client.newWebSocketBuilder()
                .buildAsync(URI.create(step.url), listener)
                .thenCompose(webSocket -> {
                    CompletableFuture<WebSocket> sendFuture = CompletableFuture.completedFuture(webSocket);

                    if (step.message != null && !step.message.isBlank()) {
                        sendFuture = webSocket.sendText(step.message, true)
                                .thenApply(ignored -> webSocket);
                    }

                    return sendFuture.thenCompose(ws ->
                            listener.getMessageFuture()
                                    .orTimeout(waitTime, TimeUnit.MILLISECONDS)
                                    .thenCompose(receivedMessage -> {
                                        boolean success = step.expectedMessage == null
                                                || receivedMessage.contains(step.expectedMessage);

                                        long responseTime = System.currentTimeMillis() - startTime;

                                        return ws.sendClose(WebSocket.NORMAL_CLOSURE, "done")
                                                .thenApply(ignored -> new RequestMetric(
                                                        userId,
                                                        personaName,
                                                        step.name,
                                                        success ? 101 : 500,
                                                        responseTime,
                                                        success
                                                ));
                                    })
                    );
                })
                .exceptionally(error -> new RequestMetric(
                        userId,
                        personaName,
                        step.name,
                        0,
                        System.currentTimeMillis() - startTime,
                        false
                ));
    }

    private static class WebSocketListener implements WebSocket.Listener {

        private final CompletableFuture<String> messageFuture = new CompletableFuture<>();

        public CompletableFuture<String> getMessageFuture() {
            return messageFuture;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageFuture.complete(data.toString());
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            messageFuture.completeExceptionally(error);
        }
    }
}
