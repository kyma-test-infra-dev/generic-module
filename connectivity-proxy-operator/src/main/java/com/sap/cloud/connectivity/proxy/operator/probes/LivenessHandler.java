package com.sap.cloud.connectivity.proxy.operator.probes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.javaoperatorsdk.operator.Operator;

public class LivenessHandler implements HttpHandler {

	private final Operator operator;

	public LivenessHandler(Operator operator) {
		this.operator = operator;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (operator.getRuntimeInfo().allEventSourcesAreHealthy()) {
			sendMessage(exchange, 200, "Healthy!");
		} else {
			sendMessage(exchange, 500, "An event source is not healthy!");
		}
	}

	private void sendMessage(HttpExchange httpExchange, int code, String message) throws IOException {
		try (var outputStream = httpExchange.getResponseBody()) {
			byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
			httpExchange.sendResponseHeaders(code, bytes.length);
			outputStream.write(bytes);
			outputStream.flush();
		}
	}
}
