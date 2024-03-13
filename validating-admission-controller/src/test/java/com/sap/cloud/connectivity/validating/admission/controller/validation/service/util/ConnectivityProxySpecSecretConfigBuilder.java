package com.sap.cloud.connectivity.validating.admission.controller.validation.service.util;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec;

/**
 * A utility class, which can be used to set the values of the "secretConfig" section of the ConnectivityProxy custom
 * resources. It initializes some of them with a default value, according the current default "values.yaml".
 */
public class ConnectivityProxySpecSecretConfigBuilder {

	private String connectivityServiceSecretName = "connectivity-proxy-service-key";
	private String connectivityServiceSecretData = null;

	private String auditLogServiceSecretName = "auditlog-service-key";
	private String auditLogServiceSecretData = null;

	public ConnectivityProxySpec.SecretConfig build() {
		ConnectivityProxySpec.SecretConfig secretConfig = new ConnectivityProxySpec.SecretConfig();

		secretConfig.setIntegration(createIntegration());

		return secretConfig;
	}

	private ConnectivityProxySpec.SecretConfig.Integration createIntegration() {
		ConnectivityProxySpec.SecretConfig.Integration integration = new ConnectivityProxySpec.SecretConfig.Integration();

		integration.setConnectivityService(createConnectivityService());
		integration.setAuditlogService(createAuditLogService());

		return integration;
	}

	private ConnectivityProxySpec.SecretConfig.Integration.ConnectivityService createConnectivityService() {
		ConnectivityProxySpec.SecretConfig.Integration.ConnectivityService connectivityService = new ConnectivityProxySpec.SecretConfig.Integration.ConnectivityService();

		connectivityService.setSecretName(connectivityServiceSecretName);
		connectivityService.setSecretData(connectivityServiceSecretData);

		return connectivityService;
	}

	private ConnectivityProxySpec.SecretConfig.Integration.AuditlogService createAuditLogService() {
		ConnectivityProxySpec.SecretConfig.Integration.AuditlogService auditLogService = new ConnectivityProxySpec.SecretConfig.Integration.AuditlogService();

		auditLogService.setSecretName(auditLogServiceSecretName);
		auditLogService.setSecretData(auditLogServiceSecretData);

		return auditLogService;
	}

	public ConnectivityProxySpecSecretConfigBuilder setConnectivityServiceSecretName(String connectivityServiceSecretName) {
		this.connectivityServiceSecretName = connectivityServiceSecretName;
		return this;
	}

	public ConnectivityProxySpecSecretConfigBuilder setConnectivityServiceSecretData(String connectivityServiceSecretData) {
		this.connectivityServiceSecretData = connectivityServiceSecretData;
		return this;
	}

	public ConnectivityProxySpecSecretConfigBuilder setAuditLogServiceSecretName(String auditLogServiceSecretName) {
		this.auditLogServiceSecretName = auditLogServiceSecretName;
		return this;
	}

	public ConnectivityProxySpecSecretConfigBuilder setAuditLogServiceSecretData(String auditLogServiceSecretData) {
		this.auditLogServiceSecretData = auditLogServiceSecretData;
		return this;
	}
}
