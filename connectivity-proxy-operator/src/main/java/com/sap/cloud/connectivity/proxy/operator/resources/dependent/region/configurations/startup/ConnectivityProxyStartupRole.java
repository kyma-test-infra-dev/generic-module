package com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.startup;

import java.util.List;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ConnectivityProxyStartupRole extends ConnectivityProxyDependentResource<Role> {

	public ConnectivityProxyStartupRole(KubernetesClient kubernetesClient) {
		super(Role.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Role, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected Role desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new RoleBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withRules(createRules(connectivityProxy))
				.build();
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.startUpRbacName();
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

	private List<PolicyRule> createRules(ConnectivityProxy connectivityProxy) {
		List<String> resourceNames = List.of(
				connectivityProxy.regionConfigurationsSecretName(),
				connectivityProxy.caSecretName());

		PolicyRule getRule = new PolicyRuleBuilder()
				.withApiGroups("")
				.withResources("secrets")
				.withResourceNames(resourceNames)
				.withVerbs("get")
				.build();

		return List.of(getRule);
	}
}
