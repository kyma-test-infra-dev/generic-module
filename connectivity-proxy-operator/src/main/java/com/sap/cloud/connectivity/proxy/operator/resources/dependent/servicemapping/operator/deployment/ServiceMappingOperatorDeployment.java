package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.operator.deployment;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_PORT;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_SECRETS_TLS_VOLUME_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_SECRETS_TLS_VOLUME_PATH;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CONFIG_MAP_KEY;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CONFIG_MAP_NAME;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

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
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ServiceMappingOperatorDeployment extends ServiceMappingsDependentResource<Deployment> {

	private static final String PROBES_PATH = "/health";
	private static final Integer PROBES_PORT = 443;

	public ServiceMappingOperatorDeployment(KubernetesClient kubernetesClient) {
		super(Deployment.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Deployment, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.serviceMappingsOperatorFullName();
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected Deployment desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new DeploymentBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		Map<String, String> labels = new HashMap<>(getManagedByLabel());
		labels.put(APP_LABEL, connectivityProxy.serviceMappingsOperatorFullName());

		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(labels)
				.build();
	}

	private DeploymentSpec createSpec(ConnectivityProxy connectivityProxy) {
		return new DeploymentSpecBuilder()
				.withSelector(createSelector(connectivityProxy))
				.withReplicas(1)
				.withTemplate(createPodTemplate(connectivityProxy))
				.build();
	}

	private LabelSelector createSelector(ConnectivityProxy connectivityProxy) {
		Map<String, String> selectorLabels = createPodLabels(connectivityProxy);

		return new LabelSelectorBuilder()
				.withMatchLabels(selectorLabels)
				.build();
	}

	private PodTemplateSpec createPodTemplate(ConnectivityProxy connectivityProxy) {
		return new PodTemplateSpecBuilder()
				.withMetadata(createPodTemplateMetadata(connectivityProxy))
				.withSpec(createPodTemplateSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createPodTemplateMetadata(ConnectivityProxy connectivityProxy) {
		Map<String, String> podTemplateMetadataLabels = createPodLabels(connectivityProxy);

		return new ObjectMetaBuilder()
				.withLabels(podTemplateMetadataLabels)
				.withAnnotations(connectivityProxy.resolveIstioSidecarInjectionAnnotation())
				.build();
	}

	private Map<String, String> createPodLabels(ConnectivityProxy connectivityProxy) {
		return Map.of(APP_LABEL, connectivityProxy.serviceMappingsOperatorFullName());
	}

	private PodSpec createPodTemplateSpec(ConnectivityProxy connectivityProxy) {
		PodSpecBuilder podSpecBuilder = new PodSpecBuilder()
				.withServiceAccount(connectivityProxy.serviceMappingsServiceAccountName())
				.withPriorityClassName(connectivityProxy.priorityClassName())
				.withImagePullSecrets(List.of())
				.withContainers(createContainers(connectivityProxy))
				.withVolumes(createVolumes())
				.withNodeSelector(connectivityProxy.nodeSelectorLabels());

		Optional<String> imagePullSecret = connectivityProxy.resolveImagePullSecret(ConnectivityProxy.ImageType.MAIN);
		imagePullSecret.ifPresent(podSpecBuilder::addNewImagePullSecret);

		return podSpecBuilder.build();
	}

	private Container createContainers(ConnectivityProxy connectivityProxy) {
		return new ContainerBuilder()
				.withName(connectivityProxy.serviceMappingsOperatorFullName())
				.withImage(connectivityProxy.resolveImage(ConnectivityProxy.ImageType.MAIN))
				.withImagePullPolicy(connectivityProxy.resolveImagePullPolicy(ConnectivityProxy.ImageType.MAIN))
				.withPorts(createContainerPorts())
				.withEnv(createEnvironmentalVariables(connectivityProxy))
				.withReadinessProbe(createContainerProbe(1, 1))
				.withLivenessProbe(createContainerProbe(30, 1))
				.withVolumeMounts(createVolumeMounts())
				.withResources(createResources())
				.build();
	}

	private ResourceRequirements createResources() {
		String cpuResourceKey = "cpu";
		String memoryResourceKey = "memory";

		Map<String, Quantity> requests = Map.of(cpuResourceKey, new Quantity("100m"), memoryResourceKey, new Quantity("384M"));
		Map<String, Quantity> limits = Map.of(cpuResourceKey, new Quantity("1"), memoryResourceKey, new Quantity("1024M"));

		return new ResourceRequirementsBuilder()
				.withRequests(requests)
				.withLimits(limits)
				.build();
	}

	private List<Volume> createVolumes() {
		SecretVolumeSource secretVolumeSource = new SecretVolumeSourceBuilder()
				.withSecretName(CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME)
				.withOptional(false)
				.build();

		Volume smOperatorSecretsTls = new VolumeBuilder()
				.withName(CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_SECRETS_TLS_VOLUME_NAME)
				.withSecret(secretVolumeSource)
				.build();

		return List.of(smOperatorSecretsTls);
	}

	private ContainerPort createContainerPorts() {
		return new ContainerPortBuilder()
				.withContainerPort(CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_PORT)
				.build();
	}

	private List<EnvVar> createEnvironmentalVariables(ConnectivityProxy connectivityProxy) {
		List<EnvVar> envVars = new LinkedList<>();

		envVars.add(createEnvVar("START_APPLICATION", "servicemapping-operator"));
		envVars.add(createEnvVar("CONNECTIVITY_NAMESPACE_NAME", connectivityProxy.installationNamespace()));
		envVars.add(createEnvVar("CONNECTIVITY_PROXY_PUBLIC_HOST", connectivityProxy.externalBusinessDataTunnelHost()));
		envVars.add(createEnvVar("CONNECTIVITY_PROXY_PUBLIC_PORT", connectivityProxy.externalBusinessDataTunnelPort().toString()));

		if (connectivityProxy.getSpec().getConfig().getTenantMode().equals("dedicated")) {
			envVars.add(createEnvVar("CONNECTIVITY_PROXY_DEDICATED_TENANT", connectivityProxy.getSpec().getConfig().getSubaccountId()));
		}

		envVars.add(createEnvVar("CONNECTIVITY_SERVICEMAPPING_CONFIGMAP_NAME", SERVICE_MAPPINGS_CONFIG_MAP_NAME));
		envVars.add(createEnvVar("CONNECTIVITY_SERVICEMAPPING_CONFIGMAP_DATA_KEY", SERVICE_MAPPINGS_CONFIG_MAP_KEY));

		return envVars;
	}

	private EnvVar createEnvVar(String name, String value) {
		return new EnvVarBuilder()
				.withName(name)
				.withValue(value)
				.build();
	}

	private Probe createContainerProbe(int initialDelaySeconds, int timeoutSeconds) {
		HTTPGetAction httpGet = new HTTPGetActionBuilder()
				.withPath(PROBES_PATH)
				.withPort(new IntOrString(PROBES_PORT))
				.withScheme("HTTPS")
				.build();

		return new ProbeBuilder()
				.withHttpGet(httpGet)
				.withInitialDelaySeconds(initialDelaySeconds)
				.withTimeoutSeconds(timeoutSeconds)
				.build();
	}

	private List<VolumeMount> createVolumeMounts() {
		VolumeMount connectivitySMOperatorSecretsTls = new VolumeMountBuilder()
				.withName(CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_SECRETS_TLS_VOLUME_NAME)
				.withMountPath(CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_SECRETS_TLS_VOLUME_PATH)
				.withReadOnly(true)
				.build();

		return List.of(connectivitySMOperatorSecretsTls);
	}
}
