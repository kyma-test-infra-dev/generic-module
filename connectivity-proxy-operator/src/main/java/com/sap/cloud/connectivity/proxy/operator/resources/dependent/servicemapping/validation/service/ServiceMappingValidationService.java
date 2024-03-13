package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.validation.service;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_PORT;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_SERVICE_FULL_NAME;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;

import java.util.Map;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ServiceMappingValidationService extends ServiceMappingsDependentResource<Service> {

	public ServiceMappingValidationService(KubernetesClient kubernetesClient) {
		super(Service.class, kubernetesClient);
	}

	@Override
	protected Service desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ServiceBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Service, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_SERVICE_FULL_NAME;
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

	private ServiceSpec createSpec(ConnectivityProxy connectivityProxy) {
		return new ServiceSpecBuilder()
				.withType("ClusterIP")
				.withSelector(Map.of(APP_LABEL, connectivityProxy.serviceMappingsOperatorFullName()))
				.withPorts(createServicePort())
				.build();
	}

	private static ServicePort createServicePort() {
		return new ServicePortBuilder()
				.withName("https")
				.withProtocol("TCP")
				.withPort(CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_PORT)
				.build();
	}

}
