package com.sap.cloud.connectivity.proxy.operator.resources.dependent.restartwatcher;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_RESTART_WATCHER_LABEL;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
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

public final class RestartWatcherDeployment extends RestartWatcherDependentResource<Deployment> {

	public RestartWatcherDeployment(KubernetesClient kubernetesClient) {
		super(Deployment.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Deployment, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy ->
				new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected Deployment desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new DeploymentBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME;
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

	private DeploymentSpec createSpec(ConnectivityProxy connectivityProxy) {
		return new DeploymentSpecBuilder()
				.withReplicas(1)
				.withSelector(createLabelSelector(connectivityProxy))
				.withTemplate(createPodTemplate(connectivityProxy))
				.build();
	}

	private LabelSelector createLabelSelector(ConnectivityProxy connectivityProxy) {
		Map<String, String> labels = Map.of(APP_LABEL, getDependentResourceName(connectivityProxy));

		return new LabelSelectorBuilder()
				.withMatchLabels(labels)
				.build();
	}


	private PodTemplateSpec createPodTemplate(ConnectivityProxy connectivityProxy) {
		return new PodTemplateSpecBuilder()
				.withMetadata(createPodTemplateMetadata(connectivityProxy))
				.withSpec(createPodTemplateSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createPodTemplateMetadata(ConnectivityProxy connectivityProxy) {
		Map<String, String> labels = Map.of(APP_LABEL, getDependentResourceName(connectivityProxy));

		return new ObjectMetaBuilder()
				.withLabels(labels)
				.build();
	}

	private PodSpec createPodTemplateSpec(ConnectivityProxy connectivityProxy) {
		String restartWatcherServiceAccountName = connectivityProxy.restartWatcherServiceAccountName();

		PodSpecBuilder podSpecBuilder = new PodSpecBuilder()
				.withServiceAccount(restartWatcherServiceAccountName)
				.withPriorityClassName(connectivityProxy.priorityClassName())
				.withContainers(createPodTemplateContainers(connectivityProxy))
				.withNodeSelector(connectivityProxy.nodeSelectorLabels());

		Optional<String> imagePullSecret = connectivityProxy.resolveImagePullSecret(ConnectivityProxy.ImageType.MAIN);
		imagePullSecret.ifPresent(podSpecBuilder::addNewImagePullSecret);

		return podSpecBuilder.build();
	}

	private List<Container> createPodTemplateContainers(ConnectivityProxy connectivityProxy) {
		String containerName = getDependentResourceName(connectivityProxy);

		String image = connectivityProxy.resolveImage(ConnectivityProxy.ImageType.MAIN);
		String imagePullPolicy = connectivityProxy.resolveImagePullPolicy(ConnectivityProxy.ImageType.MAIN);

		Container restartWatcherContainer = new ContainerBuilder()
				.withName(containerName)
				.withImage(image)
				.withImagePullPolicy(imagePullPolicy)
				.withPorts(createContainerPort())
				.withEnv(createEnvironmentVariables(connectivityProxy))
				.withReadinessProbe(createReadinessProbe())
				.withLivenessProbe(createLivenessProbe())
				.build();

		return List.of(restartWatcherContainer);
	}

	private ContainerPort createContainerPort() {
		return new ContainerPortBuilder()
				.withContainerPort(80)
				.build();
	}

	private List<EnvVar> createEnvironmentVariables(ConnectivityProxy connectivityProxy) {
		EnvVar startApplication = new EnvVarBuilder()
				.withName("START_APPLICATION")
				.withValue("restart-watcher")
				.build();

		EnvVar connectivityInstallationName = new EnvVarBuilder()
				.withName("CONNECTIVITY_INSTALLATION_NAME")
				.withValue(connectivityProxy.installationName())
				.build();

		EnvVar connectivityStatefulSetName = new EnvVarBuilder()
				.withName("CONNECTIVITY_STATEFUL_SET_NAME")
				.withValue(CONNECTIVITY_PROXY_FULL_NAME)
				.build();

		EnvVar connectivityRestartLabel = new EnvVarBuilder()
				.withName("CONNECTIVITY_RESTART_LABEL")
				.withValue(CONNECTIVITY_PROXY_RESTART_WATCHER_LABEL)
				.build();

		return List.of(startApplication, connectivityInstallationName, connectivityStatefulSetName, connectivityRestartLabel);
	}

	private Probe createReadinessProbe() {
		return createProbe(1, 1);
	}

	private Probe createLivenessProbe() {
		return createProbe(30, 1);
	}

	private Probe createProbe(int initialDelaySeconds, int timeoutSeconds) {
		HTTPGetAction httpGetAction = new HTTPGetActionBuilder()
				.withPath("/health")
				.withPort(new IntOrString(8080))
				.build();

		return new ProbeBuilder()
				.withHttpGet(httpGetAction)
				.withInitialDelaySeconds(initialDelaySeconds)
				.withTimeoutSeconds(timeoutSeconds)
				.build();
	}
}
