package com.sap.cloud.connectivity.validating.admission.controller.validation.service;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CDR_NAME;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec.Config.Servers;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec.Config.Servers.Proxy.Authorization;
import com.sap.cloud.connectivity.validating.admission.controller.mapper.ConnectivityProxyMapper;
import com.sap.cloud.connectivity.validating.admission.controller.validation.service.exception.AdmissionReviewNotAllowedException;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

@Service
public class ConnectivityProxyValidator {

	private static final Logger LOGGER = LogManager.getLogger(ConnectivityProxyValidator.class);

	private final KubernetesClient kubernetesClient;

	@Autowired
	public ConnectivityProxyValidator(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public enum Operation {
		CREATE, UPDATE, DELETE, CONNECT
	}

	public enum TenantMode {
		DEDICATED("dedicated"), SHARED("shared");

		private final String mode;

		TenantMode(String mode) {
			this.mode = mode;
		}

		public String getMode() {
			return mode;
		}
	}

	public enum IngressClassName {
		NGINX("nginx"), ISTIO("istio");

		private final String className;

		IngressClassName(String className) {
			this.className = className;
		}

		public String getClassName() {
			return className;
		}
	}

	public AdmissionReview validate(AdmissionReview admissionReview) {
		AdmissionRequest admissionRequest = admissionReview.getRequest();

		AdmissionResponse response = validate(admissionRequest);
		response.setUid(admissionRequest.getUid());

		AdmissionReview responseAdmissionReview = new AdmissionReview();
		responseAdmissionReview.setResponse(response);

		return responseAdmissionReview;
	}

	private AdmissionResponse validate(AdmissionRequest admissionRequest) {
		try {
			String requestResource = admissionRequest.getResource().getResource();
			Operation operation = Operation.valueOf(admissionRequest.getOperation());
			String requestUid = admissionRequest.getUid();
			LOGGER.info("Received admission request with uid {} and operation {} for resource name {}.", requestUid, operation, requestResource);

			/*
			 * based on the value of the operation, the values for the object properties are the following:
			 * CREATE: oldObject is null, object contains the custom resource, posted to the API
			 * UPDATE: oldObject is the current object in the cluster, object contains the custom resource, posted to the API
			 * DELETE: oldObject is the current object in the cluster, object is null
			 */
			ConnectivityProxy oldConnectivityProxy = ConnectivityProxyMapper.map(admissionRequest.getOldObject());
			ConnectivityProxy newConnectivityProxy = ConnectivityProxyMapper.map(admissionRequest.getObject());

			if (operation == Operation.CREATE || operation == Operation.UPDATE) {
				doValidate(oldConnectivityProxy, newConnectivityProxy);
			}

			if (operation == Operation.UPDATE) {
				validateConnectivityProxySecretProperties(oldConnectivityProxy, newConnectivityProxy);

				if (oldConnectivityProxy.serviceChannelsEnabled() && !newConnectivityProxy.serviceChannelsEnabled()) {
					validateAllServiceMappingsAreRemovedWhenDisablingServiceChannelsDuringUpdate();
				}
			}

			if (operation == Operation.DELETE) {
				if (oldConnectivityProxy.serviceChannelsEnabled()) {
					verifyAllServiceMappingsAreRemovedBeforeDeletion();
				}
			}

			LOGGER.info("Successfully validated admission request with uuid {} and operation {}.", requestUid, operation);
			return allowAdmissionRequest();
		} catch (AdmissionReviewNotAllowedException exception) {
			LOGGER.warn("Admission request validation failed due to: ", exception);
			return denyAdmissionRequest(exception.getMessage());
		}
	}

	private void doValidate(ConnectivityProxy oldConnectivityProxy, ConnectivityProxy newConnectivityProxy)
			throws AdmissionReviewNotAllowedException {
		validateAuthorizationProperty(newConnectivityProxy);
		validateDedicatedTenantModeRequiredProperties(newConnectivityProxy);
		validateMultiRegionRequiredProperties(newConnectivityProxy);
		validateIngressProperties(newConnectivityProxy);
	}

	private void validateConnectivityProxySecretProperties(ConnectivityProxy oldConnectivityProxy, ConnectivityProxy newConnectivityProxy)
			throws AdmissionReviewNotAllowedException {
		String oldSecretData = oldConnectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService().getSecretData();
		String newSecretData = newConnectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService().getSecretData();

		boolean oldSecretDataPresent = (oldSecretData != null) && !oldSecretData.isBlank();
		boolean newSecretDataPresent = (newSecretData != null) && !newSecretData.isBlank();

		if (oldSecretDataPresent && newSecretDataPresent) {
			String oldConnectivityServiceSecretName = oldConnectivityProxy.connectivityServiceSecretName();
			String newConnectivityServiceSecretName = newConnectivityProxy.connectivityServiceSecretName();

			if (!oldConnectivityServiceSecretName.equals(newConnectivityServiceSecretName)) {
				throw new AdmissionReviewNotAllowedException("Modifying connectivityService secret name is not allowed.");
			}
		}
	}

	private void validateAllServiceMappingsAreRemovedWhenDisablingServiceChannelsDuringUpdate() throws AdmissionReviewNotAllowedException {
		if (!getAllServiceMappings().isEmpty()) {
			throw new AdmissionReviewNotAllowedException(
					"There are existing service mappings in the cluster. Please remove them before disabling the service channels property!");
		}
	}

	private void verifyAllServiceMappingsAreRemovedBeforeDeletion() throws AdmissionReviewNotAllowedException {
		if (!getAllServiceMappings().isEmpty()) {
			throw new AdmissionReviewNotAllowedException(
					"There are existing service mappings in the cluster. Please remove them, before uninstalling the Connectivity Proxy!");
		}
	}

	private List<GenericKubernetesResource> getAllServiceMappings() {
		CustomResourceDefinitionContext serviceMappingsCRDContext = new CustomResourceDefinitionContext.Builder()
				.withName(SERVICE_MAPPINGS_CDR_NAME)
				.withKind("ServiceMapping")
				.withGroup("connectivityproxy.sap.com")
				.withScope("Cluster")
				.withVersion("v1")
				.build();

		return kubernetesClient.genericKubernetesResources(serviceMappingsCRDContext)
							   .inAnyNamespace()
							   .list()
							   .getItems();
	}

	private void validateAuthorizationProperty(ConnectivityProxy newConnectivityProxy) throws AdmissionReviewNotAllowedException {
		Servers.Proxy proxyServers = newConnectivityProxy.getSpec().getConfig().getServers().getProxy();
		ConnectivityProxySpec.Config.MultiRegionMode multiRegionMode = newConnectivityProxy.getSpec().getConfig().getMultiRegionMode();

		if (Boolean.FALSE.equals(multiRegionMode.getEnabled()) && checkIfAnyOfProxyServersHaveProxyAuthorizationEnabled(proxyServers)) {
			Authorization authorization = proxyServers.getAuthorization();
			if (authorization == null) {
				throw new AdmissionReviewNotAllowedException("Authorization is required if proxy authorization is enabled");
			}

			Authorization.oAuth oauth = authorization.getOauth();
			if (oauth == null) {
				throw new AdmissionReviewNotAllowedException("Oauth is required if proxy authorization is enabled");
			}

			String allowedClientId = oauth.getAllowedClientId();
			if (allowedClientId == null || allowedClientId.isBlank()) {
				throw new AdmissionReviewNotAllowedException("Allowed client id is required if proxy authorization is enabled");
			}
		}
	}

	private void validateDedicatedTenantModeRequiredProperties(ConnectivityProxy newConnectivityProxy) throws AdmissionReviewNotAllowedException {
		String newProxyTenantMode = newConnectivityProxy.getSpec().getConfig().getTenantMode();
		if (!TenantMode.DEDICATED.getMode().equals(newProxyTenantMode)) {
			return;
		}

		String subaccountId = newConnectivityProxy.getSpec().getConfig().getSubaccountId();
		if (subaccountId == null) {
			throw new AdmissionReviewNotAllowedException("SubaccountId is required if tenant mode is dedicated");
		}

		Servers.Proxy proxyServers = newConnectivityProxy.getSpec().getConfig().getServers().getProxy();
		if (checkIfAnyOfProxyServersHaveProxyAuthorizationDisabled(proxyServers)) {
			String subaccountSubdomain = newConnectivityProxy.getSpec().getConfig().getSubaccountSubdomain();
			if (subaccountSubdomain == null) {
				throw new AdmissionReviewNotAllowedException(
						"Subaccount subdomain is required if tenant mode is dedicated and proxy authorization is disabled");
			}
		}
	}

	private boolean checkIfAnyOfProxyServersHaveProxyAuthorizationEnabled(Servers.Proxy proxyServers) {
		return checkEnableProxyAuthorization(proxyServers, Boolean.TRUE);
	}

	private boolean checkIfAnyOfProxyServersHaveProxyAuthorizationDisabled(Servers.Proxy proxyServers) {
		return checkEnableProxyAuthorization(proxyServers, Boolean.FALSE);
	}

	private boolean checkEnableProxyAuthorization(Servers.Proxy proxyServers, Boolean condition) {
		return condition.equals(proxyServers.getHttp().getEnableProxyAuthorization())
				|| condition.equals(proxyServers.getRfcAndLdap().getEnableProxyAuthorization())
				|| condition.equals(proxyServers.getSocks5().getEnableProxyAuthorization());
	}

	private void validateMultiRegionRequiredProperties(ConnectivityProxy newConnectivityProxy) throws AdmissionReviewNotAllowedException {
		ConnectivityProxySpec.Config.MultiRegionMode multiRegionMode = newConnectivityProxy.getSpec().getConfig().getMultiRegionMode();

		if (Boolean.TRUE.equals(multiRegionMode.getEnabled())) {
			String multiRegionModeConfigMapName = multiRegionMode.getConfigMapName();
			if ((multiRegionModeConfigMapName == null) || multiRegionModeConfigMapName.isBlank()) {
				throw new AdmissionReviewNotAllowedException("Region configurations Config Map name is required when multi region mode is enabled");
			}

			ConnectivityProxySpec.Config.ServiceChannels serviceChannels = newConnectivityProxy.getSpec().getConfig().getServiceChannels();
			if (Boolean.TRUE.equals(serviceChannels.getEnabled())) {
				throw new AdmissionReviewNotAllowedException("Multi region mode is not allowed when service channels are enabled");
			}

			String tenantMode = newConnectivityProxy.getSpec().getConfig().getTenantMode();
			if (TenantMode.DEDICATED.getMode().equals(tenantMode)) {
				throw new AdmissionReviewNotAllowedException("Multi region mode is not allowed when tenant mode is dedicated");
			}
		}
	}

	private void validateIngressProperties(ConnectivityProxy newConnectivityProxy) throws AdmissionReviewNotAllowedException {
		if (IngressClassName.ISTIO.getClassName().equals(newConnectivityProxy.getSpec().getIngress().getClassName())) {
			validateIstioProperties(newConnectivityProxy);
		}
	}

	private void validateIstioProperties(ConnectivityProxy newConnectivityProxy) throws AdmissionReviewNotAllowedException {
		ConnectivityProxySpec.Ingress.Tls ingressTls = newConnectivityProxy.getSpec().getIngress().getTls();
		if (ingressTls == null) {
			throw new AdmissionReviewNotAllowedException("TLS section in Ingress configuration is required when Istio is enabled");
		}

		String secretName = ingressTls.getSecretName();
		if (secretName == null || secretName.isBlank()) {
			throw new AdmissionReviewNotAllowedException("TLS secret name in Ingress configuration is required when Istio is enabled");
		}
	}

	private AdmissionResponse allowAdmissionRequest() {
		AdmissionResponse admissionResponse = new AdmissionResponse();
		admissionResponse.setAllowed(Boolean.TRUE);
		return admissionResponse;
	}

	private AdmissionResponse denyAdmissionRequest(String reason) {
		AdmissionResponse admissionResponse = new AdmissionResponse();
		admissionResponse.setAllowed(Boolean.FALSE);

		Status status = new Status();
		status.setMessage(reason);
		status.setCode(HttpStatus.CONFLICT.value());
		admissionResponse.setStatus(status);

		return admissionResponse;
	}

}
