package com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionContext;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class IstioDependentResource<R extends HasMetadata> extends ConnectivityProxyDependentResource<R> {

	protected IstioDependentResource(Class<R> resourceType, KubernetesClient kubernetesClient) {
		super(resourceType, kubernetesClient);
	}

	@Override
	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		return connectivityProxy.resolveIstioEnabled();
	}

}
