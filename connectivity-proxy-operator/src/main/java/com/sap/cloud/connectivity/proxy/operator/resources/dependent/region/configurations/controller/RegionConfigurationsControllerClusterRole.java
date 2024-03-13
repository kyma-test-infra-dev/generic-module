package com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.controller;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;

import java.util.ArrayList;
import java.util.List;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class RegionConfigurationsControllerClusterRole extends ConnectivityProxyDependentResource<ClusterRole> {

	public RegionConfigurationsControllerClusterRole(KubernetesClient kubernetesClient) {
		super(ClusterRole.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ClusterRole, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.regionConfigurationRbacName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		throw new UnsupportedOperationException("ClusterRole is cluster-wide resource!");
	}

	@Override
	protected ClusterRole desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ClusterRoleBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withRules(createRules(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private PolicyRule[] createRules(ConnectivityProxy connectivityProxy) {
		PolicyRule watchRule = new PolicyRuleBuilder()
				.withApiGroups("")
				.withResources("configmaps", "secrets", "pods")
				.withVerbs("get", "list", "watch")
				.build();

		List<String> editRuleResourceNames = createEditRuleResourceNames(connectivityProxy);
		PolicyRule editRule = new PolicyRuleBuilder()
				.withApiGroups("")
				.withResources("secrets")
				.withVerbs("get", "list", "update", "patch")
				.withResourceNames(editRuleResourceNames)
				.build();

		PolicyRule restartRule = new PolicyRuleBuilder()
				.withApiGroups("apps")
				.withResources("statefulsets")
				.withVerbs("get", "patch")
				.withResourceNames(CONNECTIVITY_PROXY_FULL_NAME)
				.build();

		return new PolicyRule[] { watchRule, editRule, restartRule };
	}

	private List<String> createEditRuleResourceNames(ConnectivityProxy connectivityProxy) {
		List<String> editRuleResourceNames = new ArrayList<>();

		String regionConfigurationsSecretName = connectivityProxy.regionConfigurationsSecretName();
		editRuleResourceNames.add(regionConfigurationsSecretName);

		String connectivityProxyCASecretName = connectivityProxy.caSecretName();
		editRuleResourceNames.add(connectivityProxyCASecretName);

		if (connectivityProxy.resolveIstioEnabled()) {
			String istioCASecretName = connectivityProxy.resolveIstioCASecretName();
			editRuleResourceNames.add(istioCASecretName);
		}

		return editRuleResourceNames;
	}
}
