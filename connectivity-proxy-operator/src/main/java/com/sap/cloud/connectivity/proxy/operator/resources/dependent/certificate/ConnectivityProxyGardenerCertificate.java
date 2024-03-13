package com.sap.cloud.connectivity.proxy.operator.resources.dependent.certificate;

import java.util.List;

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

public final class ConnectivityProxyGardenerCertificate extends ConnectivityProxyDependentResource<GardenerCertificate> {

	private static final String WILDCARD_DNS_PREFIX = "*.";

	public ConnectivityProxyGardenerCertificate(KubernetesClient client) {
		super(GardenerCertificate.class, client);
	}

	@Override
	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		return autoProvisionContext.isIngressTlsSecretNameAutoProvision();
	}
	
	@Override
	protected ResourceDiscriminator<GardenerCertificate, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.ingressTlsSecretName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.resolveIstioInstallationNamespace();
	}

	@Override
	protected GardenerCertificate desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		GardenerCertificate gardenerCert = new GardenerCertificate();
		gardenerCert.setMetadata(createMetadata(connectivityProxy));
		gardenerCert.setSpec(createSpec(connectivityProxy));
		return gardenerCert;
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private GardenerCertificateSpec createSpec(ConnectivityProxy connectivityProxy) {
		GardenerCertificateSpec certificateSpec = new GardenerCertificateSpec();

		String commonName = connectivityProxy.externalBusinessDataTunnelHost();
		List<String> dnsNames = List.of(WILDCARD_DNS_PREFIX + commonName);
		String secretName = connectivityProxy.ingressTlsSecretName();

		certificateSpec.setCommonName(commonName);
		certificateSpec.setDnsNames(dnsNames);
		certificateSpec.setSecretName(secretName);

		return certificateSpec;
	}
}
