package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.operator.rbac;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CONFIG_MAP_NAME;

import java.util.List;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

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

public final class ServiceMappingOperatorClusterRole extends ServiceMappingsDependentResource<ClusterRole> {

	public ServiceMappingOperatorClusterRole(KubernetesClient kubernetesClient) {
		super(ClusterRole.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ClusterRole, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.serviceMappingsRbacName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		throw new UnsupportedOperationException("ClusterRole is cluster-wide resource!");
	}

	@Override
	protected ClusterRole desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ClusterRoleBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withRules(createRules())
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private List<PolicyRule> createRules() {
		PolicyRule serviceMappingsRule = new PolicyRuleBuilder()
				.withApiGroups("connectivityproxy.sap.com")
				.withResources("servicemappings", "servicemappings/status")
				.withVerbs("*")
				.build();

		PolicyRule crdRule = new PolicyRuleBuilder()
				.withApiGroups("apiextensions.k8s.io")
				.withResources("customresourcedefinitions")
				.withVerbs("get", "list")
				.build();

		PolicyRule smConfigMapRule = new PolicyRuleBuilder()
				.withApiGroups("")
				.withResources("configmaps")
				.withResourceNames(SERVICE_MAPPINGS_CONFIG_MAP_NAME)
				.withVerbs("get", "list", "update", "patch")
				.build();

		PolicyRule configMapRule = new PolicyRuleBuilder()
				.withApiGroups("")
				.withResources("configmaps")
				.withVerbs("get", "list", "create")
				.build();

		return List.of(serviceMappingsRule, crdRule, smConfigMapRule, configMapRule);
	}
}
