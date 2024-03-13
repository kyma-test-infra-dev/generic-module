package com.sap.cloud.connectivity.proxy.operator.resources.dependent.secret;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionContext;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public final class ConnectivityProxySecret extends ConnectivityProxyDependentResource<Secret> {

	private static final String CONNECTIVITY_PROXY_OPERATOR_RESOURCE_IDENTIFIER_LABEL_KEY = "com.sap.connectivityproxy/resource-identifier";
	private static final String CONNECTIVITY_PROXY_OPERATOR_RESOURCE_IDENTIFIER_LABEL_VALUE = "connectivity-proxy-secret";

	public ConnectivityProxySecret(KubernetesClient kubernetesClient) {
		super(Secret.class, kubernetesClient);
	}

	@Override
	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		String secretData = connectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService().getSecretData();
		return !autoProvisionContext.isConnectivityServiceKeySecretNameAutoProvision() && (secretData != null) && !secretData.isBlank();
	}

	@Override
	protected ResourceDiscriminator<Secret, ConnectivityProxy> createResourceDiscriminator() {
		return new ConnectivityProxySecretResourceDiscriminator();
	}

	@Override
	protected Secret desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new SecretBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withType("Opaque")
				.withData(createData(connectivityProxy))
				.build();
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService().getSecretName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		Map<String, String> labels = new HashMap<>(getManagedByLabel());
		labels.put(CONNECTIVITY_PROXY_OPERATOR_RESOURCE_IDENTIFIER_LABEL_KEY, CONNECTIVITY_PROXY_OPERATOR_RESOURCE_IDENTIFIER_LABEL_VALUE);

		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(labels)
				.build();
	}

	private Map<String, String> createData(ConnectivityProxy connectivityProxy) {
		String serviceCredentialsKey = connectivityProxy.getSpec().getConfig().getIntegration().getConnectivityService().getServiceCredentialsKey();
		String secretData = connectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService().getSecretData();

		return Map.of(serviceCredentialsKey, secretData);
	}

	/**
	 * A custom resource discriminator used for distinguishing the Connectivity Proxy Secret as dependent resource.
	 * <p>
	 * The default approach uses the resource name and namespace, but for resources with dynamic names,
	 * this approach is not applicable.
	 * <p>
	 * This resource discriminator provides a different way for distinguishing secondary resources.
	 * It relies on the primary resource installation name, its namespace and a unique resource identifier.
	 */
	private static final class ConnectivityProxySecretResourceDiscriminator implements ResourceDiscriminator<Secret, ConnectivityProxy> {

		@Override
		public Optional<Secret> distinguish(Class<Secret> resource, ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
			return context.getSecondaryResourcesAsStream(resource).filter(secret -> {
				Map<String, String> labels = secret.getMetadata().getLabels();

				String resourceIdentifier = labels.get(CONNECTIVITY_PROXY_OPERATOR_RESOURCE_IDENTIFIER_LABEL_KEY);

				return CONNECTIVITY_PROXY_OPERATOR_RESOURCE_IDENTIFIER_LABEL_VALUE.equals(resourceIdentifier);
			}).findFirst();
		}
	}
}
