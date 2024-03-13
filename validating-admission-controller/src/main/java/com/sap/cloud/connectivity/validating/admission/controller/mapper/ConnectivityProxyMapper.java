package com.sap.cloud.connectivity.validating.admission.controller.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;

import io.fabric8.kubernetes.api.model.KubernetesResource;

public final class ConnectivityProxyMapper {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private ConnectivityProxyMapper() {
		// prevent initialization
	}

	public static ConnectivityProxy map(KubernetesResource kubernetesResource) {
		try {
			String kubernetesResourceAsString = OBJECT_MAPPER.writeValueAsString(kubernetesResource);
			return OBJECT_MAPPER.readValue(kubernetesResourceAsString, ConnectivityProxy.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Received KubernetesResource is not structured like a ConnectivityProxy", e);
		}
	}
}
