package com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.envoy.filter;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.ISTIO_INGRESS_GATEWAY_DEFAULT_SELECTOR;

import java.util.List;
import java.util.Map;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.IstioDependentResource;

import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilter;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterApplyTo;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterEnvoyConfigObjectMatch;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterEnvoyConfigObjectMatchBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterEnvoyConfigObjectMatchListener;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterEnvoyConfigObjectMatchListenerBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterEnvoyConfigObjectPatch;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterEnvoyConfigObjectPatchBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterListenerMatchBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterListenerMatchFilterChainMatchBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterListenerMatchFilterMatchBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterPatch;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterPatchBuilder;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterPatchContext;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterPatchOperation;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterSpec;
import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilterSpecBuilder;
import io.fabric8.istio.api.networking.v1alpha3.WorkloadSelector;
import io.fabric8.istio.api.networking.v1alpha3.WorkloadSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

/**
 * Envoy Filter, which is used to add the "connectivity" upgrade protocol to the configuration on the Istio ingress
 * gateway, specified by the "gatewaySelector" property. This protocol is used in the case, when the SAP
 * Cloud Connector opens a ServiceChannel of type "ABAP Cloud".
 * The following <a href="https://jira.tools.sap/browse/NGPBUG-364303">bug</a> in the Connectivity Proxy Helm chart is
 * related to it.
 */
public final class ConnectivityProxyEnvoyFilter extends IstioDependentResource<EnvoyFilter> {

	private static final String FILTER_NAME = "envoy.filters.network.http_connection_manager";
	private static final String CONNECTIVITY_PROTOCOL_NAME = "connectivity";

	public ConnectivityProxyEnvoyFilter(KubernetesClient kubernetesClient) {
		super(EnvoyFilter.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<EnvoyFilter, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.resolveIstioGatewayEnvoyFilterName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.resolveIstioInstallationNamespace();
	}

	@Override
	protected EnvoyFilter desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new EnvoyFilterBuilder()
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

	private EnvoyFilterSpec createSpec(ConnectivityProxy connectivityProxy) {
		return new EnvoyFilterSpecBuilder()
				.withWorkloadSelector(createWorkloadSelector(connectivityProxy))
				.withConfigPatches(createConnectivityProtocolConfigPatch(connectivityProxy))
				.build();
	}

	/**
	 * The following <a href="https://jira.tools.sap/browse/NGPBUG-364308">bug</a> in the Helm chart is related to the
	 * workload selector.
	 */
	private WorkloadSelector createWorkloadSelector(ConnectivityProxy connectivityProxy) {
		return new WorkloadSelectorBuilder()
				.withLabels(ISTIO_INGRESS_GATEWAY_DEFAULT_SELECTOR)
				.build();
	}

	private EnvoyFilterEnvoyConfigObjectPatch createConnectivityProtocolConfigPatch(ConnectivityProxy connectivityProxy) {
		return new EnvoyFilterEnvoyConfigObjectPatchBuilder()
				.withApplyTo(EnvoyFilterApplyTo.NETWORK_FILTER)
				.withMatch(createMatch(connectivityProxy))
				.withPatch(createConfigPatch())
				.build();
	}

	private EnvoyFilterEnvoyConfigObjectMatch createMatch(ConnectivityProxy connectivityProxy) {
		return new EnvoyFilterEnvoyConfigObjectMatchBuilder()
				.withContext(EnvoyFilterPatchContext.GATEWAY)
				.withEnvoyFilterEnvoyConfigObjectMatchListenerTypes(createPatchListener(connectivityProxy))
				.build();
	}

	private EnvoyFilterPatch createConfigPatch() {
		Map<String, Object> typedConfigValue = Map.of("@type", "type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager",
													  "upgrade_configs", List.of(Map.of("upgrade_type", CONNECTIVITY_PROTOCOL_NAME)));

		Map<String, Object> patchValue = Map.of("name", FILTER_NAME,
												"typed_config", typedConfigValue);

		return new EnvoyFilterPatchBuilder()
				.withOperation(EnvoyFilterPatchOperation.MERGE)
				.withValue(patchValue)
				.build();
	}

	private EnvoyFilterEnvoyConfigObjectMatchListener createPatchListener(ConnectivityProxy connectivityProxy) {
		String externalBusinessDataTunnelHost = connectivityProxy.externalBusinessDataTunnelHost();

		return new EnvoyFilterEnvoyConfigObjectMatchListenerBuilder()
				.withListener(new EnvoyFilterListenerMatchBuilder()
									  .withFilterChain(new EnvoyFilterListenerMatchFilterChainMatchBuilder()
															   .withSni(externalBusinessDataTunnelHost)
															   .withFilter(new EnvoyFilterListenerMatchFilterMatchBuilder()
																				   .withName(FILTER_NAME)
																				   .build())
															   .build())
									  .build())
				.build();
	}

}
