package com.sap.cloud.connectivity.validating.admission.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

@Configuration
public class Config {

	@Bean
	public KubernetesClient kubernetesClient() {
		return new KubernetesClientBuilder().build();
	}

}
