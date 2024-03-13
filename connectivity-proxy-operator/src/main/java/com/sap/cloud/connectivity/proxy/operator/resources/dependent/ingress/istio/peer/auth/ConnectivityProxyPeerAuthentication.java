package com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.peer.auth;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;

import java.util.Map;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.IstioDependentResource;

import io.fabric8.istio.api.security.v1beta1.PeerAuthentication;
import io.fabric8.istio.api.security.v1beta1.PeerAuthenticationBuilder;
import io.fabric8.istio.api.security.v1beta1.PeerAuthenticationMutualTLS;
import io.fabric8.istio.api.security.v1beta1.PeerAuthenticationMutualTLSBuilder;
import io.fabric8.istio.api.security.v1beta1.PeerAuthenticationMutualTLSMode;
import io.fabric8.istio.api.security.v1beta1.PeerAuthenticationSpec;
import io.fabric8.istio.api.security.v1beta1.PeerAuthenticationSpecBuilder;
import io.fabric8.istio.api.type.v1beta1.WorkloadSelector;
import io.fabric8.istio.api.type.v1beta1.WorkloadSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ConnectivityProxyPeerAuthentication extends IstioDependentResource<PeerAuthentication> {

	public ConnectivityProxyPeerAuthentication(KubernetesClient kubernetesClient) {
		super(PeerAuthentication.class, kubernetesClient);
	}

	@Override
	protected ResourceDiscriminator<PeerAuthentication, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_PROXY_FULL_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected PeerAuthentication desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new PeerAuthenticationBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec())
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private PeerAuthenticationSpec createSpec() {
		return new PeerAuthenticationSpecBuilder()
				.withMtls(createMTLS())
				.withSelector(createSelector())
				.build();
	}

	private WorkloadSelector createSelector() {
		// labels should match the pods of the StatefulSet
		Map<String, String> labels = Map.of(APP_LABEL, CONNECTIVITY_PROXY_FULL_NAME);

		return new WorkloadSelectorBuilder()
				.withMatchLabels(labels)
				.build();
	}

	private PeerAuthenticationMutualTLS createMTLS() {
		return new PeerAuthenticationMutualTLSBuilder()
				.withMode(PeerAuthenticationMutualTLSMode.PERMISSIVE)
				.build();
	}
}
