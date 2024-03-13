package com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.destination.rule;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.IstioDependentResource;

import io.fabric8.istio.api.networking.v1beta1.ConnectionPoolSettings;
import io.fabric8.istio.api.networking.v1beta1.ConnectionPoolSettingsBuilder;
import io.fabric8.istio.api.networking.v1beta1.ConnectionPoolSettingsTCPSettings;
import io.fabric8.istio.api.networking.v1beta1.ConnectionPoolSettingsTCPSettingsBuilder;
import io.fabric8.istio.api.networking.v1beta1.DestinationRule;
import io.fabric8.istio.api.networking.v1beta1.DestinationRuleBuilder;
import io.fabric8.istio.api.networking.v1beta1.DestinationRuleSpec;
import io.fabric8.istio.api.networking.v1beta1.DestinationRuleSpecBuilder;
import io.fabric8.istio.api.networking.v1beta1.TrafficPolicy;
import io.fabric8.istio.api.networking.v1beta1.TrafficPolicyBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

/**
 * An Istio DestinationRule, which sets a connection timeout to the headless tunnel service. The value is taken from a
 * property in the custom resource.
 */
public final class ConnectivityProxyDestinationRule extends IstioDependentResource<DestinationRule> {

	public ConnectivityProxyDestinationRule(KubernetesClient kubernetesClient) {
		super(DestinationRule.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<DestinationRule, ConnectivityProxy> createResourceDiscriminator() {
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
	protected DestinationRule desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new DestinationRuleBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private DestinationRuleSpec createSpec(ConnectivityProxy connectivityProxy) {
		return new DestinationRuleSpecBuilder()
				.withHost(connectivityProxy.tunnelServiceFQNHost())
				.withTrafficPolicy(createTrafficPolicy(connectivityProxy))
				.build();
	}

	private TrafficPolicy createTrafficPolicy(ConnectivityProxy connectivityProxy) {
		return new TrafficPolicyBuilder()
				.withConnectionPool(createConnectionPool(connectivityProxy))
				.build();
	}

	private static ConnectionPoolSettings createConnectionPool(ConnectivityProxy connectivityProxy) {
		return new ConnectionPoolSettingsBuilder()
				.withTcp(createTcpConnectionPoolSettings(connectivityProxy))
				.build();
	}

	private static ConnectionPoolSettingsTCPSettings createTcpConnectionPoolSettings(ConnectivityProxy connectivityProxy) {
		return new ConnectionPoolSettingsTCPSettingsBuilder()
				.withConnectTimeout(connectivityProxy.ingressConnectionTimeout())
				.build();
	}

}
