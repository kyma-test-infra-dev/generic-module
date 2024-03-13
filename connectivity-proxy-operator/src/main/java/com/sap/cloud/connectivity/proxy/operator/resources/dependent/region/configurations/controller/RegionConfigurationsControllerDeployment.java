package com.sap.cloud.connectivity.proxy.operator.resources.dependent.region.configurations.controller;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.INSTALLATION_NAME_LABEL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class RegionConfigurationsControllerDeployment extends ConnectivityProxyDependentResource<Deployment> {

	private static final Integer DEFAULT_REPLICAS_COUNT = 1;

	private static final String PROBES_PATH = "/health";
	private static final Integer PROBES_PORT = 8080;

	public RegionConfigurationsControllerDeployment(KubernetesClient kubernetesClient) {
		super(Deployment.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Deployment, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.regionConfigurationsControllerName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected Deployment desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new DeploymentBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createDeploymentSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private DeploymentSpec createDeploymentSpec(ConnectivityProxy connectivityProxy) {
		return new DeploymentSpecBuilder()
				.withSelector(createLabelSelector(connectivityProxy))
				.withReplicas(DEFAULT_REPLICAS_COUNT)
				.withTemplate(createPodTemplate(connectivityProxy))
				.build();
	}

	private LabelSelector createLabelSelector(ConnectivityProxy connectivityProxy) {
		return new LabelSelectorBuilder()
				.withMatchLabels(createPodMetadataLabels(connectivityProxy))
				.build();
	}

	private PodTemplateSpec createPodTemplate(ConnectivityProxy connectivityProxy) {
		return new PodTemplateSpecBuilder()
				.withMetadata(createPodTemplateMetadata(connectivityProxy))
				.withSpec(createPodTemplateSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createPodTemplateMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withLabels(createPodMetadataLabels(connectivityProxy))
				.build();
	}

	private Map<String, String> createPodMetadataLabels(ConnectivityProxy connectivityProxy) {
		String appLabelValue = getDependentResourceName(connectivityProxy);
		String installationLabelValue = connectivityProxy.installationName();

		return Map.of(
				APP_LABEL, appLabelValue,
				INSTALLATION_NAME_LABEL, installationLabelValue
		);
	}

	private PodSpec createPodTemplateSpec(ConnectivityProxy connectivityProxy) {
		String regionControllerServiceAccountName = connectivityProxy.regionConfigurationsControllerName();

		PodSpecBuilder podSpecBuilder = new PodSpecBuilder()
				.withServiceAccount(regionControllerServiceAccountName)
				.withPriorityClassName(connectivityProxy.priorityClassName())
				.withContainers(createPodTemplateContainer(connectivityProxy))
				.withNodeSelector(connectivityProxy.nodeSelectorLabels());

		Optional<String> imagePullSecret = connectivityProxy.resolveImagePullSecret(ConnectivityProxy.ImageType.MAIN);
		imagePullSecret.ifPresent(podSpecBuilder::addNewImagePullSecret);

		return podSpecBuilder.build();
	}

	private Container createPodTemplateContainer(ConnectivityProxy connectivityProxy) {
		String containerName = connectivityProxy.regionConfigurationsControllerName();
		String image = connectivityProxy.resolveImage(ConnectivityProxy.ImageType.MAIN);
		String imagePullPolicy = connectivityProxy.resolveImagePullPolicy(ConnectivityProxy.ImageType.MAIN);

		return new ContainerBuilder()
				.withName(containerName)
				.withImage(image)
				.withImagePullPolicy(imagePullPolicy)
				.withEnv(createEnv(connectivityProxy))
				.withReadinessProbe(createProbe(1, 1))
				.withLivenessProbe(createProbe(30, 1))
				.build();
	}

	private List<EnvVar> createEnv(ConnectivityProxy connectivityProxy) {
		List<EnvVar> environmentVariables = new ArrayList<>();

		environmentVariables.add(createEnvVar("START_APPLICATION", "region-configurations-controller"));
		environmentVariables.add(createEnvVar("CONNECTIVITY_PROXY_FULL_NAME", CONNECTIVITY_PROXY_FULL_NAME));
		environmentVariables.add(createEnvVar("REGION_CONFIGURATIONS_CONFIG_MAP_NAME", connectivityProxy.regionConfigurationsConfigMapName()));
		environmentVariables.add(createEnvVar("REGION_CONFIGURATIONS_SECRET_NAME", connectivityProxy.regionConfigurationsSecretName()));
		environmentVariables.add(createEnvVar("REGION_CONFIGURATIONS_SECRET_DATA_KEY", connectivityProxy.regionConfigurationsSecretDataKey()));
		environmentVariables.add(createEnvVar("CONNECTIVITY_SERVICE_CREDENTIALS_KEY", connectivityProxy.connectivityServiceCredentialsKey()));
		environmentVariables.add(createEnvVar("AUDITLOG_SERVICE_CREDENTIALS_KEY", connectivityProxy.auditLogServiceCredentialsKey()));
		environmentVariables.add(createEnvVar("CONNECTIVITY_CA_SECRET_NAME", connectivityProxy.caSecretName()));
		environmentVariables.add(createEnvVar("CONNECTIVITY_CA_SECRET_DATA_KEY", connectivityProxy.caSecretDataKey()));
		environmentVariables.add(createEnvVar("ISTIO_ENABLED", Boolean.toString(connectivityProxy.resolveIstioEnabled())));

		if (connectivityProxy.resolveIstioEnabled()) {
			environmentVariables.add(createEnvVar("ISTIO_NAMESPACE", connectivityProxy.resolveIstioInstallationNamespace()));
			environmentVariables.add(createEnvVar("ISTIO_CONNECTIVITY_CA_SECRET_NAME", connectivityProxy.resolveIstioCASecretName()));
			environmentVariables.add(createEnvVar("ISTIO_CONNECTIVITY_CA_SECRET_DATA_KEY", connectivityProxy.resolveIstioCASecretDataKey()));
		}

		return environmentVariables;
	}

	private EnvVar createEnvVar(String name, String value) {
		return new EnvVarBuilder()
				.withName(name)
				.withValue(value)
				.build();
	}

	private Probe createProbe(int initialDelaySeconds, int timeoutSeconds) {
		HTTPGetAction httpGet = new HTTPGetActionBuilder()
				.withPort(new IntOrString(PROBES_PORT))
				.withPath(PROBES_PATH)
				.build();

		return new ProbeBuilder()
				.withHttpGet(httpGet)
				.withInitialDelaySeconds(initialDelaySeconds)
				.withTimeoutSeconds(timeoutSeconds)
				.build();
	}
}
