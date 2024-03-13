package com.sap.cloud.connectivity.proxy.operator.resources.dependent.configmap;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_RESTART_WATCHER_LABEL;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectivityProxyConfig extends ConnectivityProxyDependentResource<ConfigMap> {

	private static final Logger LOGGER = LogManager.getLogger(ConnectivityProxyConfig.class);

	private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

	private static final String CONNECTIVITY_PROXY_CONFIG_DATA_KEY = "connectivity-proxy-config.yml";

	public ConnectivityProxyConfig(KubernetesClient kubernetesClient) {
		super(ConfigMap.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ConfigMap, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy ->
				new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected ConfigMap desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ConfigMapBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withData(createData(connectivityProxy))
				.build();
	}

	@Override
	public ConfigMap update(ConfigMap actual, ConfigMap target, ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		ConfigMap updated = super.update(actual, target, connectivityProxy, context);

		if (!connectivityProxy.restartWatcherEnabled()) {
			String name = getDependentResourceName(connectivityProxy);
			String namespace = getDependentResourceNamespace(connectivityProxy);

			LOGGER.info("Restarting Connectivity Proxy pods because configuration with name \"{}\" and namespace \"{}\" updated ", name, namespace);

			getKubernetesClient()
				.apps()
				.statefulSets()
				.inNamespace(namespace)
				.withName(name)
				.rolling()
				.restart();
		}

		return updated;
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		Map<String, String> labels = new HashMap<>(getManagedByLabel());
		if (connectivityProxy.restartWatcherEnabled()) {
			labels.put(CONNECTIVITY_PROXY_RESTART_WATCHER_LABEL, connectivityProxy.restartWatcherLabelValue());
		}

		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(labels)
				.build();
	}

	private Map<String, String> createData(ConnectivityProxy connectivityProxy) {
		try (StringWriter writer = new StringWriter()) {
			YAML_OBJECT_MAPPER.writeValue(writer, connectivityProxy.getSpec().getConfig());
			return Map.of(CONNECTIVITY_PROXY_CONFIG_DATA_KEY, writer.toString());
		} catch (IOException e) {
			final String message = "Failed to create Connectivity Proxy ConfigMap due to: ";
			LOGGER.error(message, e);
			throw new IllegalStateException(message, e);
		}
	}
}
