package com.sap.cloud.connectivity.proxy.operator.resources.dependent.statefulset;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_CA_SECRET_MOUNT_PATH;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_CA_SECRET_VOLUME_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_CONFIG_MOUNT_PATH;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_CONFIG_VOLUME_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_MOUNT_PATH;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_VOLUME_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_SERVICE_MAPPINGS_VOLUME_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_SERVICE_MAPPINGS_VOLUME_PATH;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CONFIG_MAP_NAME;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.ConnectivityProxyDependentResource;

import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ExecAction;
import io.fabric8.kubernetes.api.model.ExecActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.Lifecycle;
import io.fabric8.kubernetes.api.model.LifecycleBuilder;
import io.fabric8.kubernetes.api.model.LifecycleHandler;
import io.fabric8.kubernetes.api.model.LifecycleHandlerBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodAffinityTerm;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinity;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceFieldSelector;
import io.fabric8.kubernetes.api.model.ResourceFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.TCPSocketActionBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm;
import io.fabric8.kubernetes.api.model.WeightedPodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ConnectivityProxyStatefulSet extends ConnectivityProxyDependentResource<StatefulSet> {

	private static final long ISTIO_SIDECAR_PROXY_UID = 1337L;
	private static final long SECURITY_CONTEXT_UID = 1000L;
	private static final long SECURITY_CONTEXT_GID = 3000L;

	public ConnectivityProxyStatefulSet(KubernetesClient kubernetesClient) {
		super(StatefulSet.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<StatefulSet, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_PROXY_FULL_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected StatefulSet desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new StatefulSetBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private StatefulSetSpec createSpec(ConnectivityProxy connectivityProxy) {
		int replicas = connectivityProxy.getSpec().getDeployment().getReplicaCount();

		return new StatefulSetSpecBuilder()
				.withReplicas(replicas)
				.withPodManagementPolicy("OrderedReady")
				.withSelector(createLabelSelector(connectivityProxy))
				.withServiceName(CONNECTIVITY_PROXY_TUNNEL_FULL_NAME)
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
		return Map.of(APP_LABEL, appLabelValue);
	}

	private PodSpec createPodTemplateSpec(ConnectivityProxy connectivityProxy) {
		return new PodSpecBuilder()
				.withServiceAccount(connectivityProxy.startUpRbacName())
				.withPriorityClassName(connectivityProxy.priorityClassName())
				.withAutomountServiceAccountToken()
				.withTerminationGracePeriodSeconds(10L)
				.withAffinity(createPodAffinity())
				.withInitContainers(createInitContainers(connectivityProxy))
				.withContainers(createContainers(connectivityProxy))
				.withImagePullSecrets(createImagePullSecrets(connectivityProxy))
				.withVolumes(createVolumes(connectivityProxy))
				.withNodeSelector(connectivityProxy.nodeSelectorLabels())
				.build();
	}

	private Affinity createPodAffinity() {
		LabelSelectorRequirement labelSelectorRequirement = new LabelSelectorRequirementBuilder()
				.withKey(APP_LABEL)
				.withOperator("In")
				.addToValues(CONNECTIVITY_PROXY_FULL_NAME)
				.build();

		LabelSelector labelSelector = new LabelSelectorBuilder()
				.withMatchExpressions(labelSelectorRequirement)
				.build();

		PodAffinityTerm podAffinityTerm = new PodAffinityTermBuilder()
				.withLabelSelector(labelSelector)
				.withTopologyKey("topology.kubernetes.io/zone")
				.build();

		WeightedPodAffinityTerm weightedPodAffinityTerm = new WeightedPodAffinityTermBuilder()
				.withPodAffinityTerm(podAffinityTerm)
				.withWeight(100)
				.build();

		PodAntiAffinity podAntiAffinity = new PodAntiAffinityBuilder()
				.withPreferredDuringSchedulingIgnoredDuringExecution(weightedPodAffinityTerm)
				.build();

		return new AffinityBuilder()
				.withPodAntiAffinity(podAntiAffinity)
				.build();
	}

	private List<Container> createInitContainers(ConnectivityProxy connectivityProxy) {
		Container initFdLimitContainer = createInitFdLimitContainer(connectivityProxy);
		Container initStartUpConditionsContainer = createInitStartUpConditionsContainer(connectivityProxy);
		return List.of(initFdLimitContainer, initStartUpConditionsContainer);
	}

	private Container createInitFdLimitContainer(ConnectivityProxy connectivityProxy) {
		String image = connectivityProxy.resolveImage(ConnectivityProxy.ImageType.UTILITY);
		String imagePullPolicy = connectivityProxy.resolveImagePullPolicy(ConnectivityProxy.ImageType.UTILITY);

		Integer maxFileDescriptorCount = connectivityProxy.getSpec().getDeployment().getResources().getMaxFileDescriptorCount();
		Objects.requireNonNull(maxFileDescriptorCount, "maxFileDescriptorCount is null");

		String[] command = new String[]{ "sh", "-c", "ulimit -n " + maxFileDescriptorCount };

		return new ContainerBuilder()
				.withName("init-fd-limit")
				.withImage(image)
				.withImagePullPolicy(imagePullPolicy)
				.withSecurityContext(
						new SecurityContextBuilder()
						.withPrivileged(true)
						.build())
				.withCommand(command)
				.build();
	}

	private Container createInitStartUpConditionsContainer(ConnectivityProxy connectivityProxy) {
		String image = connectivityProxy.resolveImage(ConnectivityProxy.ImageType.UTILITY);
		String imagePullPolicy = connectivityProxy.resolveImagePullPolicy(ConnectivityProxy.ImageType.UTILITY);

		String[] command = new String[]{ "/bin/sh", "-c" };

		String regionConfigurationsUrl = String.format("https://kubernetes.default.svc/api/v1/namespaces/%s/secrets/%s",
				connectivityProxy.installationNamespace(), connectivityProxy.regionConfigurationsSecretName());
		String connectivityCAUrl = String.format("https://kubernetes.default.svc/api/v1/namespaces/%s/secrets/%s",
				connectivityProxy.installationNamespace(), connectivityProxy.caSecretName());

		String regionConfigurationsSecretDataKey = String.format(".data.%s", connectivityProxy.regionConfigurationsSecretDataKey());
		String caSecretDataKey = String.format(".data.\"%s\"", connectivityProxy.caSecretDataKey());

		String args =
				"SERVICEACCOUNT_DIRECTORY=\"/var/run/secrets/kubernetes.io/serviceaccount\"\n" +
				"TOKEN=\"$(cat ${SERVICEACCOUNT_DIRECTORY}/token)\"\n" +
				"K8S_CACERT=\"${SERVICEACCOUNT_DIRECTORY}/ca.crt\"\n" +
				"REGION_CONFIGURATIONS_URL=\"" + regionConfigurationsUrl + "\"\n" +
				"CONNECTIVITY_CA_URL=\"" + connectivityCAUrl + "\"\n" +
				"while [ \"e30=\" = $(curl -sS --cacert \"${K8S_CACERT}\" \"${REGION_CONFIGURATIONS_URL}\" -H \"Authorization: Bearer ${TOKEN}\" -H \"Accept: application/json\" " +
						"| jq -r '" + regionConfigurationsSecretDataKey + "') ]; " +
						"do sleep 1; echo 'Waiting for region configurations...'; " +
						"done\n" +
				"while [ -z $(curl -sS --cacert \"${K8S_CACERT}\" \"${CONNECTIVITY_CA_URL}\" -H \"Authorization: Bearer ${TOKEN}\" -H \"Accept: application/json\" " +
						"| jq -r '" + caSecretDataKey + "') ]; " +
						"do sleep 1; echo 'Waiting for Connectivity CAs...'; " +
						"done\n";

		return new ContainerBuilder()
				.withName("init-startup-conditions")
				.withImage(image)
				.withSecurityContext(new SecurityContextBuilder()
						.withRunAsUser(ISTIO_SIDECAR_PROXY_UID)
						.build())
				.withImagePullPolicy(imagePullPolicy)
				.withCommand(command)
				.withArgs(args)
				.build();
	}

	private List<Container> createContainers(ConnectivityProxy connectivityProxy) {
		String image = connectivityProxy.resolveImage(ConnectivityProxy.ImageType.MAIN);
		String pullPolicy = connectivityProxy.resolveImagePullPolicy(ConnectivityProxy.ImageType.MAIN);

		Container container = new ContainerBuilder()
				.withName(CONNECTIVITY_PROXY_FULL_NAME)
				.withImage(image)
				.withImagePullPolicy(pullPolicy)
				.withReadinessProbe(createProbe())
				.withLivenessProbe(createProbe())
				.withStartupProbe(createStartUpProbe())
				.withLifecycle(createContainerLifecycle())
				.withResources(createResourceRequirements(connectivityProxy))
				.withEnv(createEnv())
				.withSecurityContext(createSecurityContext())
				.withVolumeMounts(createVolumeMounts(connectivityProxy))
				.build();

		return List.of(container);
	}

	private Probe createProbe() {
		return new ProbeBuilder()
				.withTcpSocket(
						new TCPSocketActionBuilder()
								.withPort(new IntOrString(8042))
								.build())
				.build();
	}

	private Probe createStartUpProbe() {
		return new ProbeBuilder()
				.withTcpSocket(
						new TCPSocketActionBuilder()
								.withPort(new IntOrString(8042))
								.build())
				.withFailureThreshold(30)
				.withPeriodSeconds(10)
				.build();
	}

	private Lifecycle createContainerLifecycle() {
		ExecAction execAction = new ExecActionBuilder()
				.withCommand("/bin/bash", "-c", "sleep 20")
				.build();

		LifecycleHandler lifecycleHandler = new LifecycleHandlerBuilder()
				.withExec(execAction)
				.build();

		return new LifecycleBuilder()
				.withPreStop(lifecycleHandler)
				.build();
	}

	private ResourceRequirements createResourceRequirements(ConnectivityProxy connectivityProxy) {
		String cpuLimitKey = "cpu";
		String memoryLimitKey = "memory";

		String requestsCPU = connectivityProxy.getSpec().getDeployment().getResources().getRequests().getCPU();
		String requestsMemory = connectivityProxy.getSpec().getDeployment().getResources().getRequests().getMemory();

		Map<String, Quantity> requests = Map.of(
				cpuLimitKey, new Quantity(requestsCPU),
				memoryLimitKey, new Quantity(requestsMemory)
		);

		String limitCPU = connectivityProxy.getSpec().getDeployment().getResources().getLimits().getCPU();
		String limitMemory = connectivityProxy.getSpec().getDeployment().getResources().getLimits().getMemory();

		Map<String, Quantity> limits = Map.of(
				cpuLimitKey, new Quantity(limitCPU),
				memoryLimitKey, new Quantity(limitMemory)
		);

		return new ResourceRequirementsBuilder()
				.withRequests(requests)
				.withLimits(limits)
				.build();
	}

	private List<EnvVar> createEnv() {
		EnvVar startApplication = new EnvVarBuilder()
				.withName("START_APPLICATION")
				.withValue("connectivity-proxy")
				.build();

		ResourceFieldSelector cpuLimitResourceFieldRef = new ResourceFieldSelectorBuilder()
				.withContainerName(CONNECTIVITY_PROXY_FULL_NAME)
				.withResource("limits.cpu")
				.withDivisor(new Quantity("1m"))
				.build();

		EnvVarSource cpuLimitSource = new EnvVarSourceBuilder()
				.withResourceFieldRef(cpuLimitResourceFieldRef)
				.build();

		EnvVar cpuLimit = new EnvVarBuilder()
				.withName("CPU_LIMIT")
				.withValueFrom(cpuLimitSource)
				.build();

		return List.of(startApplication, cpuLimit);
	}

	private SecurityContext createSecurityContext() {
		return new SecurityContextBuilder()
				.withRunAsUser(SECURITY_CONTEXT_UID)
				.withRunAsGroup(SECURITY_CONTEXT_GID)
				.build();
	}

	private List<VolumeMount> createVolumeMounts(ConnectivityProxy connectivityProxy) {
		List<VolumeMount> volumeMounts = new ArrayList<>();

		volumeMounts.add(createReadOnlyVolumeMount(
				CONNECTIVITY_PROXY_CONFIG_VOLUME_NAME,
				CONNECTIVITY_PROXY_CONFIG_MOUNT_PATH));
		volumeMounts.add(createReadOnlyVolumeMount(
				CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_VOLUME_NAME,
				CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_MOUNT_PATH));
		volumeMounts.add(createReadOnlyVolumeMount(
				CONNECTIVITY_PROXY_CA_SECRET_VOLUME_NAME,
				CONNECTIVITY_PROXY_CA_SECRET_MOUNT_PATH));

		if (connectivityProxy.serviceChannelsEnabled()) {
			volumeMounts.add(createReadOnlyVolumeMount(CONNECTIVITY_PROXY_SERVICE_MAPPINGS_VOLUME_NAME, CONNECTIVITY_PROXY_SERVICE_MAPPINGS_VOLUME_PATH));
		}

		return volumeMounts;
	}

	private VolumeMount createReadOnlyVolumeMount(String volumeMountName, String volumeMountPath) {
		return new VolumeMountBuilder()
				.withName(volumeMountName)
				.withMountPath(volumeMountPath)
				.withReadOnly(true)
				.build();
	}

	private List<LocalObjectReference> createImagePullSecrets(ConnectivityProxy connectivityProxy) {
		Set<String> pullSecrets = new HashSet<>();

		Optional<String> mainImagePullSecret = connectivityProxy.resolveImagePullSecret(ConnectivityProxy.ImageType.MAIN);
		Optional<String> utilityImagePullSecret = connectivityProxy.resolveImagePullSecret(ConnectivityProxy.ImageType.UTILITY);

		mainImagePullSecret.ifPresent(pullSecrets::add);
		utilityImagePullSecret.ifPresent(pullSecrets::add);

		return pullSecrets.stream()
				.map(LocalObjectReference::new)
				.toList();
	}

	private List<Volume> createVolumes(ConnectivityProxy connectivityProxy) {
		List<Volume> connectivityProxyVolumes = new ArrayList<>();

		connectivityProxyVolumes.add(createConnectivityProxyConfigMapVolume());
		connectivityProxyVolumes.add(createRegionConfigurationsSecretVolume(connectivityProxy));
		connectivityProxyVolumes.add(createConnectivityProxyCASecretVolume(connectivityProxy));

		if (connectivityProxy.serviceChannelsEnabled()) {
			connectivityProxyVolumes.add(createServiceMappingsVolume());
		}

		return connectivityProxyVolumes;
	}

	private Volume createConnectivityProxyConfigMapVolume() {
		ConfigMapVolumeSource connectivityProxyConfigVolumeSource = new ConfigMapVolumeSourceBuilder()
				.withName(CONNECTIVITY_PROXY_FULL_NAME)
				.build();

		return new VolumeBuilder()
				.withName(CONNECTIVITY_PROXY_CONFIG_VOLUME_NAME)
				.withConfigMap(connectivityProxyConfigVolumeSource)
				.build();
	}

	private Volume createRegionConfigurationsSecretVolume(ConnectivityProxy connectivityProxy) {
		SecretVolumeSource regionConfigurationsSecretVolumeSource = new SecretVolumeSourceBuilder()
				.withSecretName(connectivityProxy.regionConfigurationsSecretName())
				.build();

		return new VolumeBuilder()
				.withName(CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_VOLUME_NAME)
				.withSecret(regionConfigurationsSecretVolumeSource)
				.build();
	}

	private Volume createConnectivityProxyCASecretVolume(ConnectivityProxy connectivityProxy) {
		SecretVolumeSource connectivityProxyCAVolumeSource = new SecretVolumeSourceBuilder()
				.withSecretName(connectivityProxy.caSecretName())
				.build();

		return new VolumeBuilder()
				.withName(CONNECTIVITY_PROXY_CA_SECRET_VOLUME_NAME)
				.withSecret(connectivityProxyCAVolumeSource)
				.build();
	}

	private Volume createServiceMappingsVolume() {
		ConfigMapVolumeSource serviceMappingsConfigMapVolumeSource = new ConfigMapVolumeSourceBuilder()
				.withName(SERVICE_MAPPINGS_CONFIG_MAP_NAME)
				.build();

		return new VolumeBuilder()
				.withName(CONNECTIVITY_PROXY_SERVICE_MAPPINGS_VOLUME_NAME)
				.withConfigMap(serviceMappingsConfigMapVolumeSource)
				.build();
	}
}
