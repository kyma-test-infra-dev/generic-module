package com.sap.cloud.connectivity.proxy.operator;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_FULL_NAME;
import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxyStatus;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionAttributeProvider;
import com.sap.cloud.connectivity.proxy.operator.provisioning.AutoProvisionContext;
import com.sap.cloud.connectivity.proxy.operator.resources.ResourceManager;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ControllerConfiguration
public class ConnectivityProxyReconciler implements Reconciler<ConnectivityProxy>, Cleaner<ConnectivityProxy>,
		EventSourceInitializer<ConnectivityProxy>, ErrorStatusHandler<ConnectivityProxy> {

	private static final Logger LOGGER = LogManager.getLogger(ConnectivityProxyReconciler.class);

	private static final int WORKLOAD_RESOURCES_READINESS_CHECK_INTERVAL = 15;

	private final ResourceManager resourceManager;
	private final AutoProvisionAttributeProvider autoProvisionAttributeProvider;

	public ConnectivityProxyReconciler(KubernetesClient client) {
		this.resourceManager = new ResourceManager(client);
		this.autoProvisionAttributeProvider = new AutoProvisionAttributeProvider(client);
	}

	@Override
	public Map<String, EventSource> prepareEventSources(EventSourceContext<ConnectivityProxy> context) {
		return EventSourceInitializer.nameEventSourcesFromDependentResource(context, resourceManager.getDependentResources());
	}

	@Override
	public UpdateControl<ConnectivityProxy> reconcile(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		LOGGER.info("Reconciliation for custom resource of kind \"{}\" with name \"{}\" in namespace \"{}\" triggered!", connectivityProxy.getKind(),
				connectivityProxy.installationName(), connectivityProxy.installationNamespace());
		AutoProvisionContext autoProvisionContext = new AutoProvisionContext(connectivityProxy);

		autoProvisionAttributeProvider.provideReconcileAttributes(connectivityProxy, autoProvisionContext);

		var resources = resourceManager.getResources();
		for (int i = 0; i < resources.size(); i++) {
			var dependentResource = resources.get(i);
			if (dependentResource.isReconcileConditionMet(connectivityProxy, autoProvisionContext)) {
				dependentResource.reconcile(connectivityProxy, context);
			} else {
				dependentResource.delete(connectivityProxy, context);
			}
		}

		if (!areWorkloadResourcesReady(connectivityProxy, context)) {
			LOGGER.info("Workload resources aren't ready yet, setting state to {} and rescheduling next reconciliation after maximum {} seconds.",
					ConnectivityProxyStatus.State.PROCESSING, WORKLOAD_RESOURCES_READINESS_CHECK_INTERVAL);
			connectivityProxy.setStatus(createStatus(ConnectivityProxyStatus.State.PROCESSING));
			return UpdateControl.patchStatus(connectivityProxy).rescheduleAfter(WORKLOAD_RESOURCES_READINESS_CHECK_INTERVAL, TimeUnit.SECONDS);
		} else {
			LOGGER.info("Workload resources are ready, setting state to {}.", ConnectivityProxyStatus.State.READY);
			connectivityProxy.setStatus(createStatus(ConnectivityProxyStatus.State.READY));
			return UpdateControl.patchStatus(connectivityProxy);
		}
	}

	private boolean areWorkloadResourcesReady(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return regionConfigurationDeploymentReady(connectivityProxy, context) && restartWatcherDeploymentReady(connectivityProxy, context)
				&& serviceChannelsDeploymentReady(connectivityProxy, context) && connectivityProxyStatefulSetReady(connectivityProxy, context);
	}

	private boolean regionConfigurationDeploymentReady(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		String regionConfigurationDeploymentName = connectivityProxy.regionConfigurationsControllerName();
		String regionConfigurationDeploymentNamespace = connectivityProxy.installationNamespace();
		return isDeploymentReady(regionConfigurationDeploymentName, regionConfigurationDeploymentNamespace, context);
	}

	private boolean restartWatcherDeploymentReady(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		String restartWatcherDeploymentName = CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME;
		String restartWatcherDeploymentNamespace = connectivityProxy.installationNamespace();
		return !connectivityProxy.restartWatcherEnabled()
				|| isDeploymentReady(restartWatcherDeploymentName, restartWatcherDeploymentNamespace, context);
	}

	private boolean serviceChannelsDeploymentReady(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		String serviceChannelsDeploymentName = connectivityProxy.serviceMappingsOperatorFullName();
		String serviceChannelsDeploymentNamespace = connectivityProxy.installationNamespace();
		return !connectivityProxy.serviceChannelsEnabled()
				|| isDeploymentReady(serviceChannelsDeploymentName, serviceChannelsDeploymentNamespace, context);
	}

	private boolean connectivityProxyStatefulSetReady(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		String connectivityProxyStatefulSetName = CONNECTIVITY_PROXY_FULL_NAME;
		String connectivityProxyStatefulSetNamespace = connectivityProxy.installationNamespace();
		return isStatefulSetReady(connectivityProxyStatefulSetName, connectivityProxyStatefulSetNamespace, context);
	}

	private boolean isDeploymentReady(String name, String namespace, Context<ConnectivityProxy> context) {
		return context.getClient().apps().deployments().inNamespace(namespace).withName(name).isReady();
	}

	private boolean isStatefulSetReady(String name, String namespace, Context<ConnectivityProxy> context) {
		return context.getClient().apps().statefulSets().inNamespace(namespace).withName(name).isReady();
	}

	@Override
	public DeleteControl cleanup(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		LOGGER.info("Cleanup for custom resource of kind \"{}\" with name \"{}\" in namespace \"{}\" triggered!", connectivityProxy.getKind(),
				connectivityProxy.installationName(), connectivityProxy.installationNamespace());
		AutoProvisionContext autoProvisionContext = new AutoProvisionContext(connectivityProxy);

		autoProvisionAttributeProvider.provideCleanupAttributes(connectivityProxy, autoProvisionContext);

		var resources = resourceManager.getResources();
		for (int i = resources.size() - 1; i >= 0; i--) {
			resources.get(i).delete(connectivityProxy, context);
		}

		return DeleteControl.defaultDelete();
	}

	@Override
	public ErrorStatusUpdateControl<ConnectivityProxy> updateErrorStatus(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context,
			Exception exception) {
		LOGGER.error("Error for custom resource of kind \"{}\" with name \"{}\" in namespace \"{}\" occurred. Setting state to {}",
				connectivityProxy.getKind(), connectivityProxy.installationName(), connectivityProxy.installationNamespace(),
				ConnectivityProxyStatus.State.ERROR);

		connectivityProxy.setStatus(createStatus(ConnectivityProxyStatus.State.ERROR));

		return ErrorStatusUpdateControl.patchStatus(connectivityProxy);
	}

	private ConnectivityProxyStatus createStatus(ConnectivityProxyStatus.State state) {
		return new ConnectivityProxyStatus(state);
	}

}
