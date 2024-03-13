package com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.secret;

import java.util.Map;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class RegionConfigurationsSecret extends ConnectivityProxyDependentResource<Secret> {

	private static final String EMPTY_JSON_STRING_BASE64_ENCODED = "e30=";

	public RegionConfigurationsSecret(KubernetesClient kubernetesClient) {
		super(Secret.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Secret, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}
	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.regionConfigurationsSecretName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	/**
	 * A workaround to be able to use an external controller, populating the secret.
	 * That way, we say, that the current state, initially just an empty JSON string, is always matching the newly updated state,
	 * which is eventually changed by the external controller.
	 *
	 * @param actualResource
	 *            the resource we want to determine whether it's matching the desired state
	 * @param connectivityProxy
	 *            the connectivityProxy resource from which the desired state is inferred
	 * @param context
	 *            the context in which the resource is being matched
	 * @return instance of Result, in which the match is always set to <b>true</b>
	 */
	@Override
	public Result<Secret> match(Secret actualResource, ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return Result.computed(true, desired(connectivityProxy, context));
	}

	@Override
	protected Secret desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new SecretBuilder()
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
		return Map.of(connectivityProxy.regionConfigurationsSecretDataKey(), EMPTY_JSON_STRING_BASE64_ENCODED);
	}
}
