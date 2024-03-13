package com.sap.cloud.connectivity.operator.core;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_CA_SECRET_DATA_KEY;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_CA_SECRET_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_HTTP_SERVER_PORT;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_DATA_KEY;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_RFC_LDAP_SERVER_PORT;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_SOCKS5_SERVER_PORT;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_TUNNEL_SERVICE_ZERO_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_SERVICE_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.DISABLE_ISTIO_SIDECAR_INJECTION_ANNOTATION;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.ISTIO_CA_SECRET_DATA_KEY;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.ISTIO_DEFAULT_INSTALLATION_NAMESPACE;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.ISTIO_INGRESS_GATEWAY_DEFAULT_SELECTOR;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.KUBERNETES_SERVICE_FQN_SUFFIX;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CONFIG_MAP_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("connectivityproxy.sap.com")
@Version("v1")
@ShortNames("cp")
public class ConnectivityProxy extends CustomResource<ConnectivityProxySpec, ConnectivityProxyStatus> implements Namespaced {

	public enum ImageType {
		UTILITY,
		MAIN
	}

	public String installationName() {
		return getMetadata().getName();
	}

	public String installationNamespace() {
		return getMetadata().getNamespace();
	}

	public boolean restartWatcherEnabled() {
		ConnectivityProxySpec.Deployment.RestartWatcher restartWatcher = getSpec().getDeployment().getRestartWatcher();
		return (restartWatcher != null) && restartWatcher.getEnabled();
	}

	public String restartWatcherLabelValue() {
		return String.format("%s.%s", installationName(), installationNamespace());
	}

	public String restartWatcherServiceAccountName() {
		return CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME;
	}

	public String restartWatcherRbacName() {
		return String.format("%s-%s", CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME, installationNamespace());
	}

	public String caSecretName() {
		return CONNECTIVITY_PROXY_CA_SECRET_NAME;
	}

	public String caSecretDataKey() {
		return CONNECTIVITY_PROXY_CA_SECRET_DATA_KEY;
	}

	public boolean multiRegionModeEnabled() {
		ConnectivityProxySpec.Config.MultiRegionMode multiRegionMode = getSpec().getConfig().getMultiRegionMode();
		return (multiRegionMode != null) && multiRegionMode.getEnabled();
	}

	public String startUpRbacName() {
		return String.format("%s-%s", CONNECTIVITY_PROXY_FULL_NAME, "startup");
	}

	public String regionConfigurationsSecretName() {
		return CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_FULL_NAME;
	}

	public String regionConfigurationsSecretDataKey() {
		return CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_DATA_KEY;
	}

	public String regionConfigurationsConfigMapName() {
		return multiRegionModeEnabled() ? getSpec().getConfig().getMultiRegionMode().getConfigMapName()
				: CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_FULL_NAME;
	}

	public String regionConfigurationsControllerName() {
		return String.format("%s-%s", CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_FULL_NAME, "controller");
	}

	public String regionConfigurationRbacName() {
		return String.format("%s-%s", regionConfigurationsControllerName(), installationNamespace());
	}

	public String resolveImage(ImageType imageType) {
		ConnectivityProxySpec.Deployment.Image image = switch (imageType) {
			case MAIN -> getSpec().getDeployment().getImage();
			case UTILITY -> getSpec().getDeployment().getUtilityImage();
		};

		String registry = image.getRegistry();
		String repository = image.getRepository();

		String tag = image.getTag();
		String digest = image.getDigest();

		String imageDefinition;
		if (digest != null && !digest.isBlank()) {
			imageDefinition = String.format("%s/%s@%s", registry, repository, digest);
		} else if (tag != null && !tag.isBlank()) {
			imageDefinition = String.format("%s/%s:%s", registry, repository, tag);
		} else {
			throw new IllegalArgumentException("No digest or tag was specified!");
		}

		return imageDefinition;
	}

	public String resolveImagePullPolicy(ImageType imageType) {
		return switch (imageType) {
			case MAIN -> getSpec().getDeployment().getImage().getPullPolicy();
			case UTILITY -> getSpec().getDeployment().getUtilityImage().getPullPolicy();
		};
	}

	public Optional<String> resolveImagePullSecret(ImageType imageType) {
		String imagePullSecret = switch (imageType) {
			case MAIN -> getSpec().getDeployment().getImage().getPullSecret();
			case UTILITY -> getSpec().getDeployment().getUtilityImage().getPullSecret();
		};

		return (imagePullSecret != null && !imagePullSecret.isBlank()) ? Optional.of(imagePullSecret) : Optional.empty();
	}

	public String ingressTlsSecretName() {
		return getSpec().getIngress().getTls().getSecretName();
	}

	public boolean resolveIstioEnabled() {
		return getSpec().getIngress().getClassName().equals("istio");
	}

	public String resolveIstioInstallationNamespace() {
		String istioInstallationNamespace = ISTIO_DEFAULT_INSTALLATION_NAMESPACE;

		ConnectivityProxySpec.Ingress.Istio istio = getSpec().getIngress().getIstio();
		if (istio != null && istio.getNamespace() != null) {
			istioInstallationNamespace = istio.getNamespace();
		}

		return istioInstallationNamespace;
	}

	public String resolveIstioCASecretName() {
		String ingressTlsSecretName = ingressTlsSecretName();
		return String.format("%s-%s", ingressTlsSecretName, ConnectivityProxyConstants.ISTIO_CA_SECRET_NAME_SUFFIX);
	}

	public String resolveIstioCASecretDataKey() {
		return ISTIO_CA_SECRET_DATA_KEY;
	}

	public Map<String, String> resolveIstioGatewaySelector() {
		Map<String, String> gatewaySelector = ISTIO_INGRESS_GATEWAY_DEFAULT_SELECTOR;

		ConnectivityProxySpec.Ingress.Istio istio = getSpec().getIngress().getIstio();
		if (istio != null && istio.getGateway() != null && istio.getGateway().getSelector() != null) {
			gatewaySelector = istio.getGateway().getSelector();
		}

		return gatewaySelector;
	}

	public Map<String, String> resolveIstioSidecarInjectionAnnotation() {
		if (resolveIstioEnabled()) {
			return DISABLE_ISTIO_SIDECAR_INJECTION_ANNOTATION;
		} else {
			return Collections.emptyMap();
		}
	}

	public List<String> resolveIstioCiphers() {
		List<String> istioCiphers = Collections.emptyList();

		ConnectivityProxySpec.Ingress.Istio istio = getSpec().getIngress().getIstio();
		if (istio != null && istio.getTls() != null && istio.getTls().getCiphers() != null) {
			istioCiphers = istio.getTls().getCiphers();
		}

		return istioCiphers;
	}

	public String resolveIstioGatewayEnvoyFilterName() {
		return String.format("%s-custom-protocol-%s", CONNECTIVITY_PROXY_FULL_NAME, installationNamespace());
	}

	public Map<String, String> ingressAnnotations() {
		Map<String, String> ingressAnnotations = getSpec().getIngress().getAnnotations();
		return Objects.requireNonNullElse(ingressAnnotations, Collections.emptyMap());
	}

	public String ingressTlsSecretAutoProvisionName() {
		return String.format("cp-tls-%s", installationIdentifier());
	}

	public String connectivityServiceKeySecretAutoProvisionName() {
		return String.format("cp-secret-%s", installationIdentifier());
	}

	public String serviceInstanceAutoProvisionName() {
		return String.format("cp-service-instance-%s", installationIdentifier());
	}

	public String serviceBindingAutoProvisionName() {
		return String.format("cp-service-binding-%s", installationIdentifier());
	}

	public String installationIdentifier() {
		return String.format("%s-%s", installationName(), installationNamespace());
	}

	public String ingressConnectionTimeout() {
		return String.format("%s%s", getSpec().getIngress().getTimeouts().getProxy().getConnect(),
							 ConnectivityProxyConstants.TIMEOUTS_SECONDS_SUFFIX);
	}

	public String ingressReadWriteTimeout() {
		ConnectivityProxySpec.Ingress.Timeouts.Proxy proxyTimeouts = getSpec().getIngress().getTimeouts().getProxy();
		return String.format("%s%s", Math.max(proxyTimeouts.getRead(), proxyTimeouts.getSend()), ConnectivityProxyConstants.TIMEOUTS_SECONDS_SUFFIX);
	}

	public String proxyServiceFQNHost() {
		return String.format("%s.%s.%s",
							 CONNECTIVITY_PROXY_FULL_NAME,
							 installationNamespace(),
							 KUBERNETES_SERVICE_FQN_SUFFIX);
	}

	public Integer rfcAndLdapProxyServerPort() {
		Integer rfcLdapPort = getSpec().getConfig().getServers().getProxy().getRfcAndLdap().getPort();

		if (rfcLdapPort == null) {
			rfcLdapPort = CONNECTIVITY_PROXY_RFC_LDAP_SERVER_PORT;
		}

		return rfcLdapPort;
	}

	public Integer httpProxyServerPort() {
		Integer httpPort = getSpec().getConfig().getServers().getProxy().getHttp().getPort();
		if (httpPort == null) {
			httpPort = CONNECTIVITY_PROXY_HTTP_SERVER_PORT;
		}

		return httpPort;
	}

	public Integer socks5ProxyPort() {
		Integer socks5Port = getSpec().getConfig().getServers().getProxy().getSocks5().getPort();

		if (socks5Port == null) {
			socks5Port = CONNECTIVITY_PROXY_SOCKS5_SERVER_PORT;
		}

		return socks5Port;
	}

	public String tunnelServiceFQNHost() {
		return String.format("%s.%s.%s",
							 CONNECTIVITY_PROXY_TUNNEL_FULL_NAME,
							 installationNamespace(),
							 KUBERNETES_SERVICE_FQN_SUFFIX);
	}

	public String tunnelServiceZeroFQNHost() {
		return String.format("%s.%s.%s",
							 CONNECTIVITY_PROXY_TUNNEL_SERVICE_ZERO_FULL_NAME,
							 installationNamespace(),
							 KUBERNETES_SERVICE_FQN_SUFFIX);
	}

	public Optional<String> auditLogServiceSecretName() {
		String auditLogMode = getSpec().getConfig().getIntegration().getAuditlog().getMode();

		boolean isAuditLogServiceMode = (auditLogMode != null) && auditLogMode.equals("service");
		if (!isAuditLogServiceMode) {
			return Optional.empty();
		}

		String auditLogServiceSecretName = getSpec().getSecretConfig().getIntegration().getAuditlogService().getSecretName();
		boolean isAuditLogSecretNamePresent = (auditLogServiceSecretName != null) && !auditLogServiceSecretName.isBlank();
		if (!isAuditLogSecretNamePresent) {
			return Optional.empty();
		}

		return Optional.of(auditLogServiceSecretName);
	}

	public Optional<String> allowedClientId() {
		ConnectivityProxySpec.Config.Servers.Proxy.Authorization authorization = getSpec().getConfig().getServers().getProxy().getAuthorization();

		if (authorization == null) {
			return Optional.empty();
		}

		ConnectivityProxySpec.Config.Servers.Proxy.Authorization.oAuth oauth = authorization.getOauth();
		String allowedClientId = oauth.getAllowedClientId();
		boolean allowedClientIdPresent = allowedClientId != null && !allowedClientId.isBlank();
		if (!allowedClientIdPresent) {
			return Optional.empty();
		}

		return Optional.of(allowedClientId);
	}

	public String externalBusinessDataTunnelHost() {
		return getSpec().getConfig().getServers().getBusinessDataTunnel().getExternalHost();
	}

	public Integer externalBusinessDataTunnelPort() {
		return getSpec().getConfig().getServers().getBusinessDataTunnel().getExternalPort();
	}

	public String connectivityServiceCredentialsKey() {
		return getSpec().getConfig().getIntegration().getConnectivityService().getServiceCredentialsKey();
	}

	public boolean serviceChannelsEnabled() {
		ConnectivityProxySpec.Config.ServiceChannels serviceChannels = getSpec().getConfig().getServiceChannels();
		return serviceChannels != null && serviceChannels.getEnabled();
	}

	public String serviceMappingsOperatorFullName() {
		return CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_FULL_NAME;
	}

	public String serviceMappingsServiceAccountName() {
		return serviceMappingsOperatorFullName();
	}

	public String serviceMappingsRbacName() {
		return String.format("%s-%s", SERVICE_MAPPINGS_CONFIG_MAP_NAME, installationNamespace());
	}

	public String createSMValidationServiceFQN() {
		return String.format("%s.%s.%s", CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_SERVICE_FULL_NAME, installationNamespace(), "svc");
	}

	public String auditLogServiceCredentialsKey() {
		return getSpec().getConfig().getIntegration().getAuditlog().getServiceCredentialsKey();
	}

	public String connectivityServiceSecretName() {
		return getSpec().getSecretConfig().getIntegration().getConnectivityService().getSecretName();
	}

	public String priorityClassName() {
		return String.format("%s-%s-priority-class", CONNECTIVITY_PROXY_FULL_NAME, installationNamespace());
	}

	public Map<String, String> nodeSelectorLabels() {
		Map<String, String> nodeSelectorLabels = getSpec().getDeployment().getNodeSelector();

		if (nodeSelectorLabels == null) {
			nodeSelectorLabels = Map.of();
		}

		return nodeSelectorLabels;
	}
}
