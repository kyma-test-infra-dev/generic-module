package com.sap.cloud.connectivity.proxy.operator.resources.dependent.service;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_SERVER_PORT;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;

import java.util.Map;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

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

public class HeadlessTunnelService extends ConnectivityProxyDependentResource<Service> {

	private static final String HEADLESS_TUNNEL_SERVICE_PORT_NAME = "tunnel";
	private static final String HEADLESS_TUNNEL_SERVICE_PORT_PROTOCOL = "TCP";

	public HeadlessTunnelService(KubernetesClient kubernetesClient) {
		super(Service.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Service, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy ->
				new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected Service desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ServiceBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec())
				.build();
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_PROXY_TUNNEL_FULL_NAME;
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

	private ServiceSpec createSpec() {
		Map<String, String> selector = Map.of(APP_LABEL, CONNECTIVITY_PROXY_FULL_NAME);
		ServicePort[] servicePorts = createServicePorts();

		return new ServiceSpecBuilder()
				.withClusterIP("None")
				.withPorts(servicePorts)
				.withSelector(selector)
				.build();
	}

	private ServicePort[] createServicePorts() {
		ServicePort tcp = new ServicePortBuilder()
				.withName(HEADLESS_TUNNEL_SERVICE_PORT_NAME)
				.withProtocol(HEADLESS_TUNNEL_SERVICE_PORT_PROTOCOL)
				.withPort(CONNECTIVITY_PROXY_TUNNEL_SERVER_PORT)
				.build();

		return new ServicePort[] { tcp };
	}
}
