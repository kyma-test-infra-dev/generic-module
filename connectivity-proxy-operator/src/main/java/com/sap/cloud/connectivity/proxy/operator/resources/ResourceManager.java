package com.sap.cloud.connectivity.proxy.operator.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.certificate.ConnectivityProxyGardenerCertificate;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.configmap.ConnectivityProxyConfig;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.configmap.ConnectivityProxyInfo;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.destination.rule.ConnectivityProxyDestinationRule;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.destination.rule.ConnectivityProxyTunnelDestinationRule;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.ingress.gateway.ConnectivityProxyIngressGateway;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.peer.auth.ConnectivityProxyPeerAuthentication;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.secret.IstioCASecret;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ingress.istio.virtual.service.ConnectivityProxyTunnelVirtualService;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.configmap.RegionConfigurationsConfigMap;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.controller.RegionConfigurationsControllerClusterRole;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.controller.RegionConfigurationsControllerClusterRoleBinding;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.controller.RegionConfigurationsControllerDeployment;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.controller.RegionConfigurationsControllerServiceAccount;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.secret.RegionConfigurationsSecret;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.startup.ConnectivityProxyStartupRole;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.startup.ConnectivityProxyStartupRoleBinding;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.startup.ConnectivityProxyStartupServiceAccount;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher.RestartWatcherClusterRole;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher.RestartWatcherClusterRoleBinding;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher.RestartWatcherDeployment;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher.RestartWatcherServiceAccount;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.scheduling.ConnectivityProxyPriorityClass;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.secret.ConnectivityProxyCASecret;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.secret.ConnectivityProxySecret;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.service.HeadlessTunnelService;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.service.ProxyService;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.service.TunnelService;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicebinding.ConnectivityProxyServiceBinding;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.serviceinstance.ConnectivityProxyServiceInstance;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.configmap.ServiceMappingsConfigMap;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd.ServiceMappingCustomResourceDefinition;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.operator.deployment.ServiceMappingOperatorDeployment;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.operator.rbac.ServiceMappingOperatorClusterRole;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.operator.rbac.ServiceMappingOperatorClusterRoleBinding;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.operator.rbac.ServiceMappingOperatorServiceAccount;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.validation.secret.ServiceMappingValidationTLSSecret;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.validation.service.ServiceMappingValidationService;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.validation.webhook.ServiceMappingValidatingWebhookConfiguration;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.statefulset.ConnectivityProxyStatefulSet;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;

public final class ResourceManager {

	private final List<ConnectivityProxyDependentResource<? extends KubernetesResource>> resources = new ArrayList<>();

	public ResourceManager(KubernetesClient client) {
		initConnectivityProxyDependentResources(client);
	}

	@SuppressWarnings("rawtypes")
	public DependentResource[] getDependentResources() {
		return resources.toArray(new DependentResource[0]);
	}

	public List<ConnectivityProxyDependentResource<? extends KubernetesResource>> getResources() {
		return Collections.unmodifiableList(resources);
	}

	private void initConnectivityProxyDependentResources(KubernetesClient client) {
		// Gardener resources
		resources.add(new ConnectivityProxyGardenerCertificate(client));

		// BTP Operator resources
		resources.add(new ConnectivityProxyServiceInstance(client));
		resources.add(new ConnectivityProxyServiceBinding(client));

		// Scheduling
		resources.add(new ConnectivityProxyPriorityClass(client));

		// Service Accounts
		resources.add(new RestartWatcherServiceAccount(client));
		resources.add(new RegionConfigurationsControllerServiceAccount(client));
		resources.add(new ConnectivityProxyStartupServiceAccount(client));
		resources.add(new ServiceMappingOperatorServiceAccount(client));

		// Secrets
		resources.add(new ConnectivityProxyCASecret(client));
		resources.add(new ConnectivityProxySecret(client));
		resources.add(new IstioCASecret(client));
		resources.add(new RegionConfigurationsSecret(client));
		resources.add(new ServiceMappingValidationTLSSecret(client));

		// Configs
		resources.add(new ConnectivityProxyConfig(client));
		resources.add(new ConnectivityProxyInfo(client));
		resources.add(new RegionConfigurationsConfigMap(client));
		resources.add(new ServiceMappingsConfigMap(client));

		// Custom Resource Definitions
		resources.add(new ServiceMappingCustomResourceDefinition(client));

		// Validating Webhook Configurations
		resources.add(new ServiceMappingValidatingWebhookConfiguration(client));

		// ClusterRoles
		resources.add(new RestartWatcherClusterRole(client));
		resources.add(new RegionConfigurationsControllerClusterRole(client));
		resources.add(new ServiceMappingOperatorClusterRole(client));

		// ClusterRoleBindings
		resources.add(new RestartWatcherClusterRoleBinding(client));
		resources.add(new RegionConfigurationsControllerClusterRoleBinding(client));
		resources.add(new ServiceMappingOperatorClusterRoleBinding(client));

		// Roles
		resources.add(new ConnectivityProxyStartupRole(client));

		// RoleBindings
		resources.add(new ConnectivityProxyStartupRoleBinding(client));

		// Services
		resources.add(new ProxyService(client));
		resources.add(new HeadlessTunnelService(client));
		resources.add(new TunnelService(client));
		resources.add(new ServiceMappingValidationService(client));

		// Deployments
		resources.add(new RestartWatcherDeployment(client));
		resources.add(new RegionConfigurationsControllerDeployment(client));
		resources.add(new ServiceMappingOperatorDeployment(client));

		// StatefulSets
		resources.add(new ConnectivityProxyStatefulSet(client));

		// Istio Resources
		resources.add(new ConnectivityProxyTunnelDestinationRule(client));
		resources.add(new ConnectivityProxyDestinationRule(client));
		resources.add(new ConnectivityProxyTunnelVirtualService(client));
		resources.add(new ConnectivityProxyIngressGateway(client));
		resources.add(new ConnectivityProxyPeerAuthentication(client));
	}
}
