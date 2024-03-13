package com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class RestartWatcherServiceAccount extends RestartWatcherDependentResource<ServiceAccount> {

	public RestartWatcherServiceAccount(KubernetesClient kubernetesClient) {
		super(ServiceAccount.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ServiceAccount, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy ->
				new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.restartWatcherServiceAccountName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected ServiceAccount desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ServiceAccountBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}
}
