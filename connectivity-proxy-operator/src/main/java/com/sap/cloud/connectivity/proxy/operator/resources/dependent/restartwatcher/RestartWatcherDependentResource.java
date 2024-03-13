package com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionContext;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class RestartWatcherDependentResource<R extends HasMetadata> extends ConnectivityProxyDependentResource<R> {

	protected RestartWatcherDependentResource(Class<R> resourceType, KubernetesClient kubernetesClient) {
		super(resourceType, kubernetesClient);
	}

	@Override
	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		return connectivityProxy.restartWatcherEnabled();
	}

}
