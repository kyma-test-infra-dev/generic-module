package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.validation.webhook;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_SERVICE_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_WEBHOOK_FULL_NAME;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.RuleWithOperations;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.RuleWithOperationsBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ServiceReferenceBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhook;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfigurationBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.WebhookClientConfig;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.WebhookClientConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ServiceMappingValidatingWebhookConfiguration extends ServiceMappingsDependentResource<ValidatingWebhookConfiguration> {

	public ServiceMappingValidatingWebhookConfiguration(KubernetesClient kubernetesClient) {
		super(ValidatingWebhookConfiguration.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<ValidatingWebhookConfiguration, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_WEBHOOK_FULL_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		throw new UnsupportedOperationException("ValidatingWebhookConfiguration is cluster-wide resource!");
	}

	@Override
	protected ValidatingWebhookConfiguration desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new ValidatingWebhookConfigurationBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withWebhooks(createWebhook(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	private ValidatingWebhook createWebhook(ConnectivityProxy connectivityProxy) {
		return new ValidatingWebhookBuilder()
				.withAdmissionReviewVersions("v1", "v1beta1")
				.withClientConfig(createClientConfig(connectivityProxy))
				.withFailurePolicy("Fail")
				.withName(connectivityProxy.createSMValidationServiceFQN())
				.withRules(createRules())
				.withSideEffects("None")
				.withTimeoutSeconds(10)
				.build();
	}

	private WebhookClientConfig createClientConfig(ConnectivityProxy connectivityProxy) {
		Secret smOperatorTLSSecret = getKubernetesClient().secrets().inNamespace(connectivityProxy.installationNamespace()).withName(CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME).get();

		if (smOperatorTLSSecret == null) {
			throw new IllegalStateException("SM Operator TLS Secret isn't present!");
		}

		String caCertificate = smOperatorTLSSecret.getData().get("ca.crt");

		return new WebhookClientConfigBuilder()
				.withService(new ServiceReferenceBuilder()
									 .withName(CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_SERVICE_FULL_NAME)
									 .withNamespace(connectivityProxy.installationNamespace())
									 .withPath("/validate")
									 .withPort(443)
									 .build())
				.withCaBundle(caCertificate)
				.build();
	}

	private RuleWithOperations createRules() {
		return new RuleWithOperationsBuilder()
				.withApiGroups("connectivityproxy.sap.com")
				.withApiVersions("v1")
				.withOperations("CREATE", "UPDATE")
				.withResources("servicemappings")
				.withScope("*")
				.build();
	}
}
