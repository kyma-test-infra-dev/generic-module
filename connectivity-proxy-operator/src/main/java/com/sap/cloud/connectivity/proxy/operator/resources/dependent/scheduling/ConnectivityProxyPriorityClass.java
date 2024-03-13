package com.sap.cloud.connectivity.proxy.operator.resources.dependent.scheduling;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.scheduling.v1.PriorityClass;
import io.fabric8.kubernetes.api.model.scheduling.v1.PriorityClassBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectivityProxyPriorityClass extends ConnectivityProxyDependentResource<PriorityClass> {

	private static final int PRIORITY_VALUE = 2000000;

	public ConnectivityProxyPriorityClass(KubernetesClient kubernetesClient) {
		super(PriorityClass.class, kubernetesClient);
	}

	@Override
	protected ResourceDiscriminator<PriorityClass, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.priorityClassName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		throw new UnsupportedOperationException("PriorityClass is cluster-wide resource!");
	}

	@Override
	protected PriorityClass desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new PriorityClassBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withValue(PRIORITY_VALUE)
				.withGlobalDefault(false)
				.withDescription("Scheduling priority of connectivity-proxy component.")
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

}
