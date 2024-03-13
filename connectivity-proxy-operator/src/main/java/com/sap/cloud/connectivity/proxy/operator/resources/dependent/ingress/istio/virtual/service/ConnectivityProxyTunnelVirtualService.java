package com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.virtual.service;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_PATH;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_PATH_ZERO;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_SERVER_PORT;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.IstioDependentResource;

import io.fabric8.istio.api.networking.v1beta1.Destination;
import io.fabric8.istio.api.networking.v1beta1.DestinationBuilder;
import io.fabric8.istio.api.networking.v1beta1.HTTPMatchRequest;
import io.fabric8.istio.api.networking.v1beta1.HTTPMatchRequestBuilder;
import io.fabric8.istio.api.networking.v1beta1.HTTPRoute;
import io.fabric8.istio.api.networking.v1beta1.HTTPRouteBuilder;
import io.fabric8.istio.api.networking.v1beta1.HTTPRouteDestination;
import io.fabric8.istio.api.networking.v1beta1.HTTPRouteDestinationBuilder;
import io.fabric8.istio.api.networking.v1beta1.PortSelectorBuilder;
import io.fabric8.istio.api.networking.v1beta1.StringMatch;
import io.fabric8.istio.api.networking.v1beta1.StringMatchBuilder;
import io.fabric8.istio.api.networking.v1beta1.StringMatchPrefixBuilder;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.api.networking.v1beta1.VirtualServiceBuilder;
import io.fabric8.istio.api.networking.v1beta1.VirtualServiceSpec;
import io.fabric8.istio.api.networking.v1beta1.VirtualServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ConnectivityProxyTunnelVirtualService extends IstioDependentResource<VirtualService> {

	public ConnectivityProxyTunnelVirtualService(KubernetesClient kubernetesClient) {
		super(VirtualService.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<VirtualService, ConnectivityProxy> createResourceDiscriminator() {
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
	protected VirtualService desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new VirtualServiceBuilder()
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

	private VirtualServiceSpec createSpec(ConnectivityProxy connectivityProxy) {
		String gatewayName = String.format("%s/%s",
										   connectivityProxy.installationNamespace(),
										   CONNECTIVITY_PROXY_TUNNEL_FULL_NAME);

		HTTPRoute tunnelHttpRoute = createTunnelHttpRoute(connectivityProxy);
		HTTPRoute connectivityHttpRoute = createConnectivityHttpRoute(connectivityProxy);

		return new VirtualServiceSpecBuilder()
				.withGateways(gatewayName)
				.withHosts(connectivityProxy.externalBusinessDataTunnelHost())
				.withHttp(tunnelHttpRoute, connectivityHttpRoute)
				.build();
	}

	private HTTPRoute createTunnelHttpRoute(ConnectivityProxy connectivityProxy) {
		HTTPMatchRequest tunnelPathZeroHttpMatch = new HTTPMatchRequestBuilder()
				.withPort(connectivityProxy.externalBusinessDataTunnelPort())
				.withUri(createHttpRoutePrefixUri(CONNECTIVITY_PROXY_TUNNEL_PATH_ZERO))
				.build();

		HTTPRouteDestination tunnelServiceZeroHttpRouteDestination = new HTTPRouteDestinationBuilder()
				.withDestination(createHttpRouteDestination(connectivityProxy.tunnelServiceZeroFQNHost()))
				.build();

		return new HTTPRouteBuilder()
				.withMatch(tunnelPathZeroHttpMatch)
				.withTimeout(connectivityProxy.ingressReadWriteTimeout())
				.withRoute(tunnelServiceZeroHttpRouteDestination)
				.build();
	}

	private HTTPRoute createConnectivityHttpRoute(ConnectivityProxy connectivityProxy) {
		HTTPMatchRequest connectivityPathHttpMatch = new HTTPMatchRequestBuilder()
				.withPort(connectivityProxy.externalBusinessDataTunnelPort())
				.withUri(createHttpRoutePrefixUri(CONNECTIVITY_PROXY_TUNNEL_PATH))
				.build();

		HTTPRouteDestination connectivityTunnelServiceHttpRouteDestination = new HTTPRouteDestinationBuilder()
				.withDestination(createHttpRouteDestination(connectivityProxy.tunnelServiceFQNHost()))
				.build();

		return new HTTPRouteBuilder()
				.withMatch(connectivityPathHttpMatch)
				.withTimeout(connectivityProxy.ingressReadWriteTimeout())
				.withRoute(connectivityTunnelServiceHttpRouteDestination)
				.build();
	}

	private static StringMatch createHttpRoutePrefixUri(String prefixValue) {
		return new StringMatchBuilder()
				.withMatchType(new StringMatchPrefixBuilder()
									   .withPrefix(prefixValue)
									   .build())
				.build();
	}

	private static Destination createHttpRouteDestination(String host) {
		return new DestinationBuilder()
				.withHost(host)
				.withPort(new PortSelectorBuilder().withNumber(CONNECTIVITY_PROXY_TUNNEL_SERVER_PORT).build())
				.build();
	}

}
