package com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;

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

public final class RestartWatcherClusterRole extends RestartWatcherDependentResource<ClusterRole> {

	public RestartWatcherClusterRole(KubernetesClient kubernetesClient) {
		super(ClusterRole.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ClusterRole, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy ->
				new ResourceID(getDependentResourceName(connectivityProxy)));
	}

	@Override
	protected ClusterRole desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ClusterRoleBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withRules(createRules())
				.build();
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.restartWatcherRbacName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		throw new UnsupportedOperationException("ClusterRole is cluster-wide resource!");
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private PolicyRule[] createRules() {
		PolicyRule watchRule = new PolicyRuleBuilder()
				.withApiGroups("")
				.withResources("configmaps", "secrets")
				.withVerbs("get", "list", "watch")
				.build();

		PolicyRule restartRule = new PolicyRuleBuilder()
				.withApiGroups("apps")
				.withResources("statefulsets")
				.withVerbs("get", "patch")
				.build();

		return new PolicyRule[] { watchRule, restartRule };
	}
}
