package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.operator.rbac;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ServiceMappingOperatorClusterRoleBinding extends ServiceMappingsDependentResource<ClusterRoleBinding> {

	public ServiceMappingOperatorClusterRoleBinding(KubernetesClient kubernetesClient) {
		super(ClusterRoleBinding.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ClusterRoleBinding, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy)));
	}

	@Override
	protected ClusterRoleBinding desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ClusterRoleBindingBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSubjects(createSubjects(connectivityProxy))
				.withRoleRef(createRoleRef(connectivityProxy))
				.build();
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.serviceMappingsRbacName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		throw new UnsupportedOperationException("ClusterRoleBinding is cluster-wide resource!");
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private Subject createSubjects(ConnectivityProxy connectivityProxy) {
		String serviceMappingsServiceAccountName = connectivityProxy.serviceMappingsServiceAccountName();
		String serviceMappingsServiceAccountNamespace = connectivityProxy.installationNamespace();

		return new SubjectBuilder()
				.withKind("ServiceAccount")
				.withName(serviceMappingsServiceAccountName)
				.withNamespace(serviceMappingsServiceAccountNamespace)
				.build();
	}

	private RoleRef createRoleRef(ConnectivityProxy connectivityProxy) {
		String serviceMappingsClusterRoleName = connectivityProxy.serviceMappingsRbacName();

		return new RoleRefBuilder()
				.withKind("ClusterRole")
				.withApiGroup("rbac.authorization.k8s.io")
				.withName(serviceMappingsClusterRoleName)
				.build();
	}
}
