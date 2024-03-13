package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.configmap;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CONFIG_MAP_KEY;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CONFIG_MAP_NAME;

import java.util.Map;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ServiceMappingsConfigMap extends ServiceMappingsDependentResource<ConfigMap> {

	private static final String EMPTY_JSON_STRING = "{}";

	public ServiceMappingsConfigMap(KubernetesClient kubernetesClient) {
		super(ConfigMap.class, kubernetesClient);
	}

	@Override
	public Result<ConfigMap> match(ConfigMap actualResource, ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return Result.computed(true, desired(connectivityProxy, context));
	}

	@Override
	protected ConfigMap desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ConfigMapBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withData(createData())
				.build();
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ConfigMap, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return SERVICE_MAPPINGS_CONFIG_MAP_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private Map<String, String> createData() {
		return Map.of(SERVICE_MAPPINGS_CONFIG_MAP_KEY, EMPTY_JSON_STRING);
	}

}
