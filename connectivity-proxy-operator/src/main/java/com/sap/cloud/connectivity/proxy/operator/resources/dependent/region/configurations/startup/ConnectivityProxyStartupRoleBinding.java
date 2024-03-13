package com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.startup;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ConnectivityProxyStartupRoleBinding extends ConnectivityProxyDependentResource<RoleBinding> {

	public ConnectivityProxyStartupRoleBinding(KubernetesClient kubernetesClient) {
		super(RoleBinding.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<RoleBinding, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected RoleBinding desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new RoleBindingBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSubjects(createSubjects(connectivityProxy))
				.withRoleRef(createRoleRef(connectivityProxy))
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

	private Subject createSubjects(ConnectivityProxy connectivityProxy) {
		String startUpServiceAccountName = connectivityProxy.startUpRbacName();

		return new SubjectBuilder()
				.withKind("ServiceAccount")
				.withName(startUpServiceAccountName)
				.build();
	}

	private RoleRef createRoleRef(ConnectivityProxy connectivityProxy) {
		String startUpRoleName = connectivityProxy.startUpRbacName();

		return new RoleRefBuilder()
				.withKind("Role")
				.withApiGroup("rbac.authorization.k8s.io")
				.withName(startUpRoleName)
				.build();
	}
}
