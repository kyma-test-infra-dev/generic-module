package com.sap.cloud.connectivity.proxy.operator.resources.dependent.configmap;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;

import java.util.Map;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class ConnectivityProxyInfo extends ConnectivityProxyDependentResource<ConfigMap> {

	public ConnectivityProxyInfo(KubernetesClient kubernetesClient) {
		super(ConfigMap.class, kubernetesClient);
	}

	@Override
	protected ResourceDiscriminator<ConfigMap, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return String.format("%s-info", CONNECTIVITY_PROXY_FULL_NAME);
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected ConfigMap desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ConfigMapBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withData(createData(connectivityProxy))
				.build();

	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();

	}

	private Map<String, String> createData(ConnectivityProxy connectivityProxy) {
		String httpProxyServerPort = connectivityProxy.httpProxyServerPort().toString();
		String rfcAndLdapProxyPort = connectivityProxy.rfcAndLdapProxyServerPort().toString();
		String socks5ProxyPort = connectivityProxy.socks5ProxyPort().toString();

		return Map.of(
				"onpremise_proxy_host", connectivityProxy.proxyServiceFQNHost(),
				"onpremise_proxy_http_port", httpProxyServerPort,
				"onpremise_proxy_ldap_port", rfcAndLdapProxyPort,
				"onpremise_proxy_port", httpProxyServerPort,
				"onpremise_proxy_rfc_port", rfcAndLdapProxyPort,
				"onpremise_socks5_proxy_port", socks5ProxyPort
				);
	}
}
