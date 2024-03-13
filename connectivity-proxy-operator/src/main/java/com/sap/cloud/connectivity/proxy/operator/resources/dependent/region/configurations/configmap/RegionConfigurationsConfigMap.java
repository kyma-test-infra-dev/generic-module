package com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.configmap;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_FULL_NAME;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionContext;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

/**
 * Conditionally created ConfigMap that provides default region configurations
 * when the multi-region mode is disabled.
 */
public final class RegionConfigurationsConfigMap extends ConnectivityProxyDependentResource<ConfigMap> {

	private static final String REGION_CONFIGURATIONS_DEFAULT_CONFIG_MAP_KEY = "default";

	private static final Type MAP_TYPE_TOKEN = new TypeToken<HashMap<String, Object>>(){}.getType();
	private static final Gson GSON = new Gson();

	public RegionConfigurationsConfigMap(KubernetesClient kubernetesClient) {
		super(ConfigMap.class, kubernetesClient);
	}

	@Override
	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		return !connectivityProxy.multiRegionModeEnabled();
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ConfigMap, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_FULL_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected ConfigMap desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ConfigMapBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withData(createData(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private Map<String, String> createData(ConnectivityProxy connectivityProxy) {
		Map<String, String> dependencies = new HashMap<>();
		dependencies.put("connectivity", connectivityProxy.connectivityServiceSecretName());

		Optional<String> auditLogServiceSecretName = connectivityProxy.auditLogServiceSecretName();
		auditLogServiceSecretName.ifPresent(secretName -> dependencies.put("auditLog", secretName));

		Map<String, Object> defaultRegionValue = new HashMap<>();
		defaultRegionValue.put("dependencies", dependencies);
		Optional<String> allowedClientId = connectivityProxy.allowedClientId();
		allowedClientId.ifPresent(clientId -> defaultRegionValue.put("allowedClientId", clientId));

		return Map.of(REGION_CONFIGURATIONS_DEFAULT_CONFIG_MAP_KEY, GSON.toJson(defaultRegionValue, MAP_TYPE_TOKEN));
	}

}
