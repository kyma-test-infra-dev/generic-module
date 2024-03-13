package com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.ingress.gateway;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_FULL_NAME;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.IstioDependentResource;

import io.fabric8.istio.api.networking.v1beta1.Gateway;
import io.fabric8.istio.api.networking.v1beta1.GatewayBuilder;
import io.fabric8.istio.api.networking.v1beta1.GatewaySpec;
import io.fabric8.istio.api.networking.v1beta1.GatewaySpecBuilder;
import io.fabric8.istio.api.networking.v1beta1.Port;
import io.fabric8.istio.api.networking.v1beta1.PortBuilder;
import io.fabric8.istio.api.networking.v1beta1.Server;
import io.fabric8.istio.api.networking.v1beta1.ServerBuilder;
import io.fabric8.istio.api.networking.v1beta1.ServerTLSSettings;
import io.fabric8.istio.api.networking.v1beta1.ServerTLSSettingsBuilder;
import io.fabric8.istio.api.networking.v1beta1.ServerTLSSettingsTLSProtocol;
import io.fabric8.istio.api.networking.v1beta1.ServerTLSSettingsTLSmode;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ConnectivityProxyIngressGateway extends IstioDependentResource<Gateway> {

	public ConnectivityProxyIngressGateway(KubernetesClient kubernetesClient) {
		super(Gateway.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Gateway, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_PROXY_TUNNEL_FULL_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected Gateway desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new GatewayBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withAnnotations(connectivityProxy.ingressAnnotations())
				.withLabels(getManagedByLabel())
				.build();
	}

	private GatewaySpec createSpec(ConnectivityProxy connectivityProxy) {
		return new GatewaySpecBuilder()
				.withSelector(connectivityProxy.resolveIstioGatewaySelector())
				.withServers(createConnectivityProxyServerProperty(connectivityProxy))
				.build();
	}

	private static Server createConnectivityProxyServerProperty(ConnectivityProxy connectivityProxy) {
		return new ServerBuilder()
				.withHosts(connectivityProxy.externalBusinessDataTunnelHost())
				.withPort(createServerPort(connectivityProxy))
				.withTls(createServerTlsSettings(connectivityProxy))
				.build();
	}

	private static Port createServerPort(ConnectivityProxy connectivityProxy) {
		return new PortBuilder()
				.withName("tunnel")
				.withNumber(connectivityProxy.externalBusinessDataTunnelPort())
				.withProtocol("HTTPS")
				.build();
	}

	private static ServerTLSSettings createServerTlsSettings(ConnectivityProxy connectivityProxy) {
		return new ServerTLSSettingsBuilder()
				.withCredentialName(connectivityProxy.ingressTlsSecretName())
				.withMinProtocolVersion(ServerTLSSettingsTLSProtocol.TLSV1_2)
				.withCipherSuites(connectivityProxy.resolveIstioCiphers())
				.withMode(ServerTLSSettingsTLSmode.MUTUAL)
				.build();
	}

}
