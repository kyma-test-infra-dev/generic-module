package com.sap.cloud.connectivity.proxy.operator.resources.dependent.service;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
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

public class ProxyService extends ConnectivityProxyDependentResource<Service> {

	public ProxyService(KubernetesClient kubernetesClient) {
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
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_PROXY_FULL_NAME;
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
		Map<String, String> selector = Map.of(APP_LABEL, getDependentResourceName(connectivityProxy));
		ServicePort[] servicePorts = new ServicePort[] {
				createHttpPort(connectivityProxy),
				createRfcLdapPort(connectivityProxy),
				createSocks5Port(connectivityProxy)
		};

		return new ServiceSpecBuilder()
				.withSelector(selector)
				.withPorts(servicePorts)
				.build();
	}

	private ServicePort createHttpPort(ConnectivityProxy connectivityProxy) {
		return new ServicePortBuilder()
				.withName("https")
				.withProtocol("TCP")
				.withPort(connectivityProxy.httpProxyServerPort())
				.build();
	}

	private ServicePort createRfcLdapPort(ConnectivityProxy connectivityProxy) {
		return new ServicePortBuilder()
				.withName("rfc-and-ldap")
				.withProtocol("TCP")
				.withPort(connectivityProxy.rfcAndLdapProxyServerPort())
				.withAppProtocol("tcp")
				.build();
	}

	private ServicePort createSocks5Port(ConnectivityProxy connectivityProxy) {
		return new ServicePortBuilder()
				.withName("socks5")
				.withProtocol("TCP")
				.withPort(connectivityProxy.socks5ProxyPort())
				.withAppProtocol("tcp")
				.build();
	}
}
