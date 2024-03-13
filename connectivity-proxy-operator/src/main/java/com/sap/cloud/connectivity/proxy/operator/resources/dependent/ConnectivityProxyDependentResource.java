package com.sap.cloud.connectivity.proxy.operator.resources.dependent;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionContext;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDNoGCKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfigBuilder;

/**
 * A specific label selector is set to the configuration of the InformerEventSource for the resource. That
 * implies, that the same label key and value must be set in the metadata of every dependent resource.
 */
public abstract class ConnectivityProxyDependentResource<R extends HasMetadata>
		extends CRUDNoGCKubernetesDependentResource<R, ConnectivityProxy> {

	private static final Logger LOGGER = LogManager.getLogger(ConnectivityProxyDependentResource.class);

	private static final String MANAGED_BY_LABEL_KEY = "app.kubernetes.io/managed-by";
	private static final String MANAGED_BY_LABEL_VALUE = "sap.connectivity.proxy.operator";
	private static final String OPERATOR_LABEL_SELECTOR = String.format("%s=%s", MANAGED_BY_LABEL_KEY, MANAGED_BY_LABEL_VALUE);
	private static final Map<String, String> MANAGED_BY_LABEL = Map.of(MANAGED_BY_LABEL_KEY, MANAGED_BY_LABEL_VALUE);

	protected Map<String, String> getManagedByLabel() {
		return MANAGED_BY_LABEL;
	}

	protected ConnectivityProxyDependentResource(Class<R> resourceType, KubernetesClient kubernetesClient) {
		super(resourceType);
		super.setKubernetesClient(kubernetesClient);
		super.setResourceDiscriminator(createResourceDiscriminator());
		super.configureWith(new KubernetesDependentResourceConfigBuilder<R>().withLabelSelector(OPERATOR_LABEL_SELECTOR).build());
	}

	@Override
	public R create(R target, ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		LOGGER.info("Resource of kind \"{}\" with name \"{}\" in namespace \"{}\" will be created!",
				target.getKind(),
				target.getMetadata().getName(),
				target.getMetadata().getNamespace());
		return super.create(target, connectivityProxy, context);
	}

	@Override
	public R update(R actual, R target, ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		LOGGER.info("Resource of kind \"{}\" with name \"{}\" in namespace \"{}\" will be updated!",
					 actual.getKind(),
					 actual.getMetadata().getName(),
					 actual.getMetadata().getNamespace());
		return super.update(actual, target, connectivityProxy, context);
	}

	@Override
	protected void handleDelete(ConnectivityProxy connectivityProxy, R secondary, Context<ConnectivityProxy> context) {
		if (secondary != null) {
			LOGGER.info("Resource of kind \"{}\" with name \"{}\" in namespace \"{}\" will be deleted!",
						 secondary.getKind(),
						 secondary.getMetadata().getName(),
						 secondary.getMetadata().getNamespace());
		}
		super.handleDelete(connectivityProxy, secondary, context);
	}

	public boolean isReconcileConditionMet(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		return true;
	}

	protected abstract ResourceDiscriminator<R, ConnectivityProxy> createResourceDiscriminator();

	protected abstract String getDependentResourceName(ConnectivityProxy connectivityProxy);

	protected abstract String getDependentResourceNamespace(ConnectivityProxy connectivityProxy);
}
