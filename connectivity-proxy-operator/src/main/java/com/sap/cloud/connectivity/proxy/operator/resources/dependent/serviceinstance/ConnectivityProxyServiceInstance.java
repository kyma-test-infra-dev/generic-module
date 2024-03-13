package com.sap.cloud.connectivity.proxy.operator.resources.dependent.serviceinstance;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionContext;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ConnectivityProxyServiceInstance extends ConnectivityProxyDependentResource<ServiceInstance> {

	public ConnectivityProxyServiceInstance(KubernetesClient client) {
		super(ServiceInstance.class, client);
	}

	@Override
	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		return autoProvisionContext.isConnectivityServiceKeySecretNameAutoProvision();
	}
	
	@Override
	protected ResourceDiscriminator<ServiceInstance, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.serviceInstanceAutoProvisionName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected ServiceInstance desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		ServiceInstance serviceInstance = new ServiceInstance();

		serviceInstance.setMetadata(createMetadata(connectivityProxy));
		serviceInstance.setSpec(createSpec(connectivityProxy));

		return serviceInstance;
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private ServiceInstanceSpec createSpec(ConnectivityProxy connectivityProxy) {
		ServiceInstanceSpec serviceInstanceSpec = new ServiceInstanceSpec();

		serviceInstanceSpec.setExternalName(getDependentResourceName(connectivityProxy));
		serviceInstanceSpec.setServiceOfferingName("connectivity");
		serviceInstanceSpec.setServicePlanName("connectivity_proxy");

		return serviceInstanceSpec;
	}
}
