package com.sap.cloud.connectivity.validating.admission.controller.validation.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec;
import com.sap.cloud.connectivity.validating.admission.controller.validation.service.ConnectivityProxyValidator.TenantMode;
import com.sap.cloud.connectivity.validating.admission.controller.validation.service.util.ConnectivityProxySpecConfigBuilder;
import com.sap.cloud.connectivity.validating.admission.controller.validation.service.util.ConnectivityProxySpecIngressBuilder;
import com.sap.cloud.connectivity.validating.admission.controller.validation.service.util.ConnectivityProxySpecSecretConfigBuilder;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.GroupVersionResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@RunWith(MockitoJUnitRunner.class)
public class ConnectivityProxyValidatorTest {

	private static final ConnectivityProxySpec OLD_CONNECTIVITY_PROXY_SPEC = new ConnectivityProxySpec();

	private static final ConnectivityProxySpec NEW_CONNECTIVITY_PROXY_SPEC = new ConnectivityProxySpec();

	@Mock
	private AdmissionReview admissionReviewMock;

	@Mock
	private AdmissionRequest admissionRequestMock;

	@Mock
	private GroupVersionResource groupVersionResourceMock;

	@Mock
	private ConnectivityProxy newConnectivityProxyMock;

	@Mock
	private ConnectivityProxy oldConnectivityProxyMock;

	@Mock
	private ObjectMeta objectMetaMock;

	@Mock
	private KubernetesClient kubernetesClientMock;

	@Mock
	private MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> mixedOperationMock;

	@Mock
	private NonNamespaceOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> anyNamespaceOperationMock;

	@Mock
	private GenericKubernetesResourceList genericKubernetesResourceListMock;

	@Mock
	private List<GenericKubernetesResource> genericKubernetesResourceListContentsMock;

	private ConnectivityProxyValidator connectivityProxyValidator;

	private ConnectivityProxySpecConfigBuilder connectivityProxySpecConfigBuilder;
	private ConnectivityProxySpecConfigBuilder oldConnectivityProxySpecConfigBuilder;

	private ConnectivityProxySpecSecretConfigBuilder connectivityProxySpecSecretConfigBuilder;
	private ConnectivityProxySpecSecretConfigBuilder oldConnectivityProxySpecSecretConfigBuilder;

	private ConnectivityProxySpecIngressBuilder connectivityProxySpecIngressBuilder;

	@Before
	public void setUp() {
		when(admissionReviewMock.getRequest()).thenReturn(admissionRequestMock);
		when(admissionRequestMock.getUid()).thenReturn("uuidTest");
		when(groupVersionResourceMock.getResource()).thenReturn("connectivityproxies");
		when(admissionRequestMock.getResource()).thenReturn(groupVersionResourceMock);
		when(objectMetaMock.getName()).thenReturn("cpTestName");

		when(kubernetesClientMock.genericKubernetesResources(any())).thenReturn(mixedOperationMock);
		when(mixedOperationMock.inAnyNamespace()).thenReturn(anyNamespaceOperationMock);
		when(anyNamespaceOperationMock.list()).thenReturn(genericKubernetesResourceListMock);
		when(genericKubernetesResourceListMock.getItems()).thenReturn(genericKubernetesResourceListContentsMock);
		when(genericKubernetesResourceListContentsMock.isEmpty()).thenReturn(true);

		connectivityProxyValidator = new ConnectivityProxyValidator(kubernetesClientMock);

		connectivityProxySpecConfigBuilder = new ConnectivityProxySpecConfigBuilder();
		oldConnectivityProxySpecConfigBuilder = new ConnectivityProxySpecConfigBuilder();
		connectivityProxySpecSecretConfigBuilder = new ConnectivityProxySpecSecretConfigBuilder();
		oldConnectivityProxySpecSecretConfigBuilder = new ConnectivityProxySpecSecretConfigBuilder();

		connectivityProxySpecIngressBuilder = new ConnectivityProxySpecIngressBuilder();
	}

	@Test
	public void testWhenCreatingValidConnectivityProxySingleTenantTrustedValidationIsSuccess() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subaccountSubdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingValidConnectivityProxySingleTenantUntrustedValidationIsSuccess() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subaccountSubdomain")
																				.httpProxyAuthorization(true)
																				.allowedClientId("clientId")
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxySingleTenantTrustedWithoutSubaccountSubdomainValidationFails() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				.subaccountId("subaccount")
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxySingleTenantTrustedWithoutSubaccountValidationFails() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxySingleTenantUntrustedWithoutAuthorizationOAuthClientIdValidationFails() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subaccountSubdomain")
																				.httpProxyAuthorization(true)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenDeletingConnectivityProxyWithEnabledServiceChannelsIsSuccess() {
		setUpDeleteAdmissionRequest();

		ConnectivityProxySpec.Config config = oldConnectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				   .subaccountId("subaccount")
																				   .subaccountSubdomain("subaccountSubdomain")
																				   .setProxyServersAuthorization(false, false, false)
																				   .setServiceChannelsEnabled(true)
																				   .build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupOldConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertAllowedAdmissionResponse();
	}

	@Test
	public void testWhenDeletingConnectivityProxyWithEnabledServiceChannelIfServiceMappingsAreNotRemovedValidationFails() {
		setUpDeleteAdmissionRequest();
		when(genericKubernetesResourceListContentsMock.isEmpty()).thenReturn(false);

		ConnectivityProxySpec.Config config = oldConnectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				   .subaccountId("subaccount")
																				   .subaccountSubdomain("subaccountSubdomain")
																				   .setProxyServersAuthorization(false, false, false)
																				   .setServiceChannelsEnabled(true)
																				   .build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupOldConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenUpdatingConnectivityProxyWithEnabledServiceChannelsToDisabledIsSuccess() {
		setUpUpdateAdmissionRequest();

		ConnectivityProxySpec.Config oldConfig = oldConnectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				  .subaccountId("subaccount")
																				  .subaccountSubdomain("subaccountSubdomain")
																				  .setProxyServersAuthorization(false, false, false)
																				  .setServiceChannelsEnabled(true).build();


		ConnectivityProxySpec.Config newConfig = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				   .subaccountId("subaccount")
																				   .subaccountSubdomain("subaccountSubdomain")
																				   .setProxyServersAuthorization(false, false, false)
																				   .setServiceChannelsEnabled(false)
																				   .build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupOldConnectivityProxySpec(oldConfig, secretConfig, ingress);
		setupNewConnectivityProxySpec(newConfig, secretConfig, ingress);

		validateAndAssertAllowedAdmissionResponse();
	}

	@Test
	public void testWhenUpdatingConnectivityProxyWithEnabledServiceChannelsToDisabledIfServiceMappingsAreNotRemovedValidationFails() {
		setUpUpdateAdmissionRequest();
		when(genericKubernetesResourceListContentsMock.isEmpty()).thenReturn(false);


		ConnectivityProxySpec.Config oldConfig = oldConnectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				  .subaccountId("subaccount")
																				  .subaccountSubdomain("subaccountSubdomain")
																				  .setProxyServersAuthorization(false, false, false)
																				  .setServiceChannelsEnabled(true)
																				  .build();


		ConnectivityProxySpec.Config newConfig = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				   .subaccountId("subaccount")
																				   .subaccountSubdomain("subaccountSubdomain")
																				   .setProxyServersAuthorization(false, false, false)
																				   .setServiceChannelsEnabled(false)
																				   .build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupOldConnectivityProxySpec(oldConfig, secretConfig, ingress);
		setupNewConnectivityProxySpec(newConfig, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxyWithEnabledMultiRegionModeValid() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.setMultiRegionModeEnabled(true)
																				.setMultiRegionModeConfigMapName("multiRegionConfigMap")
																				.setServiceChannelsEnabled(false)
																				.tenantMode(TenantMode.SHARED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxyWithEnabledMultiRegionModeWithDedicatedTenantValidationFails() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.setMultiRegionModeEnabled(true)
																				.setMultiRegionModeConfigMapName("multiRegionConfigMap")
																				.setServiceChannelsEnabled(false)
																				.tenantMode(TenantMode.DEDICATED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxyWithEnabledMultiRegionModeWithNullConfigMapName() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.setMultiRegionModeEnabled(true)
																				.setMultiRegionModeConfigMapName(null)
																				.setServiceChannelsEnabled(false)
																				.tenantMode(TenantMode.SHARED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxyWithEnabledMultiRegionModeWithBlankConfigMapName() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.setMultiRegionModeEnabled(true)
																				.setMultiRegionModeConfigMapName("		 ")
																				.setServiceChannelsEnabled(false)
																				.tenantMode(TenantMode.SHARED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxyWithEnabledMultiRegionModeAndEnabledServiceChannels() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.setMultiRegionModeEnabled(true)
																				.setMultiRegionModeConfigMapName("multiRegionConfigMap")
																				.setServiceChannelsEnabled(true)
																				.tenantMode(TenantMode.SHARED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();
		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupNewConnectivityProxySpec(config, secretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenUpdatingConnectivityProxySecretNameWithSecretDataPresent() {
		setUpUpdateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();

		ConnectivityProxySpec.SecretConfig oldSecretConfig = oldConnectivityProxySpecSecretConfigBuilder.setConnectivityServiceSecretName("conn-proxy-secret-name-1")
																										.setConnectivityServiceSecretData("conn-proxy-secret-data")
																										.build();

		ConnectivityProxySpec.SecretConfig newSecretConfig = connectivityProxySpecSecretConfigBuilder.setConnectivityServiceSecretName("conn-proxy-secret-name-2")
																									 .setConnectivityServiceSecretData("conn-proxy-secret-data")
																									 .build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupOldConnectivityProxySpec(config, oldSecretConfig, ingress);
		setupNewConnectivityProxySpec(config, newSecretConfig, ingress);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenUpdatingConnectivityProxySecretNameWithNoSecretDataPresent() {
		setUpUpdateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();

		ConnectivityProxySpec.SecretConfig oldSecretConfig = oldConnectivityProxySpecSecretConfigBuilder.setConnectivityServiceSecretName("conn-proxy-secret-name-1")
																										.build();

		ConnectivityProxySpec.SecretConfig newSecretConfig = connectivityProxySpecSecretConfigBuilder.setConnectivityServiceSecretName("conn-proxy-secret-name-2")
																									 .build();
		ConnectivityProxySpec.Ingress ingress = buildValidIstioIngressProperty();

		setupOldConnectivityProxySpec(config, oldSecretConfig, ingress);
		setupNewConnectivityProxySpec(config, newSecretConfig, ingress);

		validateAndAssertAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxyWithIstioIngressWithoutTlsSecretNameValidationFails() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();

		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingressWithoutTlsSecretName = connectivityProxySpecIngressBuilder.className("istio")
																									   .build();

		setupNewConnectivityProxySpec(config, secretConfig, ingressWithoutTlsSecretName);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	@Test
	public void testWhenCreatingConnectivityProxyWithIstioIngressTlsSecretNameIsBlankValidationFails() {
		setUpCreateAdmissionRequest();

		ConnectivityProxySpec.Config config = connectivityProxySpecConfigBuilder.tenantMode(TenantMode.DEDICATED)
																				.subaccountId("subaccount")
																				.subaccountSubdomain("subdomain")
																				.setProxyServersAuthorization(false, false, false)
																				.build();

		ConnectivityProxySpec.SecretConfig secretConfig = connectivityProxySpecSecretConfigBuilder.build();
		ConnectivityProxySpec.Ingress ingressWithoutTlsSecretName = connectivityProxySpecIngressBuilder.className("istio")
																									   .tlsSecretName("\t\t")
																									   .build();

		setupNewConnectivityProxySpec(config, secretConfig, ingressWithoutTlsSecretName);

		validateAndAssertNotAllowedAdmissionResponse();
	}

	private void setUpCreateAdmissionRequest() {
		when(admissionRequestMock.getOperation()).thenReturn(ConnectivityProxyValidator.Operation.CREATE.toString());
		when(admissionRequestMock.getObject()).thenReturn(newConnectivityProxyMock);
		when(newConnectivityProxyMock.getSpec()).thenReturn(NEW_CONNECTIVITY_PROXY_SPEC);
		when(newConnectivityProxyMock.getMetadata()).thenReturn(objectMetaMock);
	}

	private void setUpDeleteAdmissionRequest() {
		when(admissionRequestMock.getOperation()).thenReturn(ConnectivityProxyValidator.Operation.DELETE.toString());
		when(admissionRequestMock.getOldObject()).thenReturn(oldConnectivityProxyMock);
		when(oldConnectivityProxyMock.getSpec()).thenReturn(OLD_CONNECTIVITY_PROXY_SPEC);
		when(oldConnectivityProxyMock.getMetadata()).thenReturn(objectMetaMock);
	}

	private void setUpUpdateAdmissionRequest() {
		when(admissionRequestMock.getOperation()).thenReturn(ConnectivityProxyValidator.Operation.UPDATE.toString());

		when(admissionRequestMock.getOldObject()).thenReturn(oldConnectivityProxyMock);
		when(oldConnectivityProxyMock.getSpec()).thenReturn(OLD_CONNECTIVITY_PROXY_SPEC);
		when(oldConnectivityProxyMock.getMetadata()).thenReturn(objectMetaMock);

		when(admissionRequestMock.getObject()).thenReturn(newConnectivityProxyMock);
		when(newConnectivityProxyMock.getSpec()).thenReturn(NEW_CONNECTIVITY_PROXY_SPEC);

		when(newConnectivityProxyMock.getMetadata()).thenReturn(objectMetaMock);
	}

	private void setupOldConnectivityProxySpec(ConnectivityProxySpec.Config config, ConnectivityProxySpec.SecretConfig secretConfig,
			ConnectivityProxySpec.Ingress connectivityProxySpecIngress) {
		OLD_CONNECTIVITY_PROXY_SPEC.setConfig(config);
		OLD_CONNECTIVITY_PROXY_SPEC.setSecretConfig(secretConfig);
		OLD_CONNECTIVITY_PROXY_SPEC.setIngress(connectivityProxySpecIngress);
	}

	private void setupNewConnectivityProxySpec(ConnectivityProxySpec.Config config, ConnectivityProxySpec.SecretConfig secretConfig,
			ConnectivityProxySpec.Ingress connectivityProxySpecIngress) {
		NEW_CONNECTIVITY_PROXY_SPEC.setConfig(config);
		NEW_CONNECTIVITY_PROXY_SPEC.setSecretConfig(secretConfig);
		NEW_CONNECTIVITY_PROXY_SPEC.setIngress(connectivityProxySpecIngress);
	}

	private void validateAndAssertAllowedAdmissionResponse() {
		AdmissionReview admissionReview = validate();

		assertTrue(admissionReview.getResponse().getAllowed());
	}

	private void validateAndAssertNotAllowedAdmissionResponse() {
		AdmissionReview admissionReview = validate();

		assertFalse(admissionReview.getResponse().getAllowed());
		assertEquals(Integer.valueOf(HttpStatus.CONFLICT.value()), admissionReview.getResponse().getStatus().getCode());
	}

	private AdmissionReview validate() {
		return connectivityProxyValidator.validate(admissionReviewMock);
	}

	private ConnectivityProxySpec.Ingress buildValidIstioIngressProperty() {
		return connectivityProxySpecIngressBuilder.className(ConnectivityProxyValidator.IngressClassName.ISTIO.getClassName())
												  .tlsSecretName("tls-secret")
												  .build();
	}
}
