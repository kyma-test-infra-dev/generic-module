package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicebinding;

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

public final class ConnectivityProxyServiceBinding extends ConnectivityProxyDependentResource<ServiceBinding> {

	public ConnectivityProxyServiceBinding(KubernetesClient client) {
		super(ServiceBinding.class, client);
	}
	
	@Override
	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		return autoProvisionContext.isConnectivityServiceKeySecretNameAutoProvision();
	}

	@Override
	protected ResourceDiscriminator<ServiceBinding, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.serviceBindingAutoProvisionName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected ServiceBinding desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		ServiceBinding serviceBinding = new ServiceBinding();

		serviceBinding.setMetadata(createMetadata(connectivityProxy));
		serviceBinding.setSpec(createSpec(connectivityProxy));

		return serviceBinding;
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private ServiceBindingSpec createSpec(ConnectivityProxy connectivityProxy) {
		ServiceBindingSpec serviceBindingSpec = new ServiceBindingSpec();

		serviceBindingSpec.setExternalName(getDependentResourceName(connectivityProxy));
		serviceBindingSpec.setServiceInstanceName(connectivityProxy.serviceInstanceAutoProvisionName());
		serviceBindingSpec.setSecretName(connectivityProxy.connectivityServiceSecretName());
		serviceBindingSpec.setSecretKey(connectivityProxy.connectivityServiceCredentialsKey());

		return serviceBindingSpec;
	}
}
