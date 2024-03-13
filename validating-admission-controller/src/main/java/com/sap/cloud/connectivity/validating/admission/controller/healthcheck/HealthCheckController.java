package com.sap.cloud.connectivity.validating.admission.controller.healthcheck;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

	private static final Map<String, String> HEALTHY_STATUS_RESPONSE = Map.of("status", "healthy");

	@GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<Map<String, String>> doHealthCheck() {
		return ResponseEntity.ok(HEALTHY_STATUS_RESPONSE);
	}

}
