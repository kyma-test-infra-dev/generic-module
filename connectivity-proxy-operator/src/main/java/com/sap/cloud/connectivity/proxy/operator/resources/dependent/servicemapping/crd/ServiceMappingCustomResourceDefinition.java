package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.SERVICE_MAPPINGS_CDR_NAME;
import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.APP_LABEL;
import static com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd.ServiceMappingCustomResourceDefinition.SpecProperty.INTERNAL_ADDRESS;
import static com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd.ServiceMappingCustomResourceDefinition.SpecProperty.LOCATION_IDS;
import static com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd.ServiceMappingCustomResourceDefinition.SpecProperty.SERVICE_ID;
import static com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd.ServiceMappingCustomResourceDefinition.SpecProperty.SUBACCOUNT_ID;
import static com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd.ServiceMappingCustomResourceDefinition.SpecProperty.TENANT_ID;
import static com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.crd.ServiceMappingCustomResourceDefinition.SpecProperty.TYPE;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionNamesBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionSpecBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceSubresourceStatusBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceSubresources;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceSubresourcesBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceValidation;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceValidationBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsOrArray;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsOrArrayBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ServiceMappingCustomResourceDefinition extends ServiceMappingsDependentResource<CustomResourceDefinition> {

	private static final String STRING_SCHEMA_TYPE = "string";
	private static final String OBJECT_SCHEMA_TYPE = "object";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final List<JsonNode> ALLOWED_PROTOCOL_TYPES_ENUM_VALUES = List.of(OBJECT_MAPPER.valueToTree("RFC"),
			OBJECT_MAPPER.valueToTree("JDBC"), OBJECT_MAPPER.valueToTree("TCP"));

	private static final String INTERNAL_ADDRESS_PROPERTY_PATTERN = "[^\\:]+:([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";

	public static class OpenApiV3SchemaProperty {
		public static final String SPEC_PROPERTY = "spec";
		public static final String STATUS_PROPERTY = "status";
	}

	public static class SpecProperty {
		public static final String TYPE = "type";
		public static final String SUBACCOUNT_ID = "subaccountId";
		public static final String SERVICE_ID = "serviceId";
		public static final String TENANT_ID = "tenantId";
		public static final String INTERNAL_ADDRESS = "internalAddress";
		public static final String LOCATION_IDS = "locationIds";
	}

	public static class StatusProperty {
		public static final String ENDPOINT = "endpoint";
	}

	public ServiceMappingCustomResourceDefinition(KubernetesClient kubernetesClient) {
		super(CustomResourceDefinition.class, kubernetesClient);
	}

	@Override
	protected ResourceIDMatcherDiscriminator<CustomResourceDefinition, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return SERVICE_MAPPINGS_CDR_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		throw new UnsupportedOperationException("CRD resources are cluster-scoped!");
	}

	@Override
	protected CustomResourceDefinition desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new CustomResourceDefinitionBuilder()
				.withMetadata(createMetadata(connectivityProxy))
				.withSpec(createSpec(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		Map<String, String> labels = new HashMap<>(getManagedByLabel());
		labels.put(APP_LABEL, CONNECTIVITY_PROXY_FULL_NAME);

		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withLabels(labels)
				.build();
	}

	private CustomResourceDefinitionSpec createSpec(ConnectivityProxy connectivityProxy) {
		return new CustomResourceDefinitionSpecBuilder()
				.withGroup("connectivityproxy.sap.com")
				.withScope("Cluster")
				.withNames(new CustomResourceDefinitionNamesBuilder()
								   .withKind("ServiceMapping")
								   .withPlural("servicemappings")
								   .withSingular("servicemapping")
								   .withShortNames("sm")
								   .build())
				.withVersions(createVersion(connectivityProxy))
				.build();
	}

	private CustomResourceDefinitionVersion createVersion(ConnectivityProxy connectivityProxy) {
		return new CustomResourceDefinitionVersionBuilder()
				.withName("v1")
				.withServed(true)
				.withStorage(true)
				.withSchema(createCustomResourceDefinitionSchema(connectivityProxy))
				.withSubresources(createCustomResourceSubresources())
				.build();
	}

	private CustomResourceValidation createCustomResourceDefinitionSchema(ConnectivityProxy connectivityProxy) {
		return new CustomResourceValidationBuilder()
				.withOpenAPIV3Schema(createOpenAPIV3SchemaProperties(connectivityProxy))
				.build();
	}

	private CustomResourceSubresources createCustomResourceSubresources() {
		return new CustomResourceSubresourcesBuilder()
				.withStatus(new CustomResourceSubresourceStatusBuilder().build())
				.build();
	}

	private JSONSchemaProps createOpenAPIV3SchemaProperties(ConnectivityProxy connectivityProxy) {
		Map<String, JSONSchemaProps> schemaProperties = Map.of(OpenApiV3SchemaProperty.SPEC_PROPERTY, createSpecProperty(connectivityProxy),
				OpenApiV3SchemaProperty.STATUS_PROPERTY, createStatusProperty());

		return new JSONSchemaPropsBuilder()
				.withType(OBJECT_SCHEMA_TYPE)
				.withProperties(schemaProperties)
				.withRequired(OpenApiV3SchemaProperty.SPEC_PROPERTY)
				.build();
	}

	private JSONSchemaProps createSpecProperty(ConnectivityProxy connectivityProxy) {
		return new JSONSchemaPropsBuilder()
				.withType(OBJECT_SCHEMA_TYPE)
				.withProperties(createSpecProperties())
				.withRequired(createSpecPropertyRequiredProperties(connectivityProxy))
				.build();
	}

	private JSONSchemaProps createStatusProperty() {
		return new JSONSchemaPropsBuilder()
				.withType(OBJECT_SCHEMA_TYPE)
				.withProperties(Map.of(StatusProperty.ENDPOINT, new JSONSchemaPropsBuilder()
						.withType(STRING_SCHEMA_TYPE)
						.withDescription("Endpoint of the service mapping")
						.build()))
				.withRequired(StatusProperty.ENDPOINT)
				.build();
	}

	private Map<String, JSONSchemaProps> createSpecProperties() {
		return Map.of(
				TYPE, createTypePropertySchema(),
				SUBACCOUNT_ID, createSubaccountPropertySchema(),
				SERVICE_ID, createServiceIdPropertySchema(),
				TENANT_ID, createTenantIDPropertySchema(),
				INTERNAL_ADDRESS, createInternalAddressPropertySchema(),
				LOCATION_IDS, createLocationIdsPropertySchema()
		);
	}

	private JSONSchemaProps createTypePropertySchema() {
		return new JSONSchemaPropsBuilder()
				.withType(STRING_SCHEMA_TYPE)
				.withEnum(ALLOWED_PROTOCOL_TYPES_ENUM_VALUES)
				.withDescription("Type of the service mapping")
				.build();
	}

	private JSONSchemaProps createSubaccountPropertySchema() {
		return new JSONSchemaPropsBuilder()
				.withType(STRING_SCHEMA_TYPE)
				.withDescription("SubaccoundId of the service mapping")
				.build();
	}

	private JSONSchemaProps createServiceIdPropertySchema() {
		return new JSONSchemaPropsBuilder()
				.withType(STRING_SCHEMA_TYPE)
				.withDescription("ServiceId of the service mapping")
				.build();
	}

	private JSONSchemaProps createTenantIDPropertySchema() {
		return new JSONSchemaPropsBuilder()
				.withType(STRING_SCHEMA_TYPE)
				.withDescription("TenantId of the service mapping")
				.build();
	}

	private JSONSchemaProps createInternalAddressPropertySchema() {
		return new JSONSchemaPropsBuilder()
				.withType(STRING_SCHEMA_TYPE)
				.withDescription("Internal address of the service mapping")
				.withPattern(INTERNAL_ADDRESS_PROPERTY_PATTERN)
				.build();
	}

	private JSONSchemaProps createLocationIdsPropertySchema() {
		return new JSONSchemaPropsBuilder()
				.withType("array")
				.withDescription("Cloud Connector location ids")
				.withItems(createLocationIdsItemsProperty())
				.build();
	}

	private JSONSchemaPropsOrArray createLocationIdsItemsProperty() {
		return new JSONSchemaPropsOrArrayBuilder()
				.withSchema(new JSONSchemaPropsBuilder()
									.withType(STRING_SCHEMA_TYPE)
									.build())
				.build();
	}

	private List<String> createSpecPropertyRequiredProperties(ConnectivityProxy connectivityProxy) {
		List<String> required = new LinkedList<>();
		required.add(TYPE);
		required.add(SERVICE_ID);
		required.add(INTERNAL_ADDRESS);

		if (connectivityProxy.getSpec().getConfig().getTenantMode().equals("shared")) {
			required.add(SUBACCOUNT_ID);
		}
		return required;
	}

}
