package com.sap.cloud.connectivity.proxy.operator.provisioning;

import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

public class AutoProvisionAttributeProvider {

	private static final Logger LOGGER = LogManager.getLogger(AutoProvisionAttributeProvider.class);

	private static final Gson GSON = new Gson();

	private final KubernetesClient client;

	public AutoProvisionAttributeProvider(KubernetesClient client) {
		this.client = client;
	}

	public void provideReconcileAttributes(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		provideIngressTlsAttributes(connectivityProxy, autoProvisionContext);
		provideConnectivityServiceKeySecretName(connectivityProxy, autoProvisionContext);
		provideConnectivityServiceKeyAttributes(connectivityProxy, autoProvisionContext);
	}

	public void provideCleanupAttributes(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		if (autoProvisionContext.isIngressTlsSecretNameAutoProvision()) {
			provideIngressTlsSecretName(connectivityProxy);
		}
	}

	private void provideIngressTlsAttributes(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		if (autoProvisionContext.isIngressTlsSecretNameAutoProvision()) {
			provideIngressTlsSecretName(connectivityProxy);

			if (autoProvisionContext.isExternalBusinessHostAutoProvision()) {
				ConfigMap shootInfoConfigMap = client.configMaps().inNamespace("kube-system").withName("shoot-info").get();
				String shootDomain = shootInfoConfigMap.getData().get("domain");
				String businessDataTunnelHost = "cp." + shootDomain;

				connectivityProxy.getSpec().getConfig().getServers().getBusinessDataTunnel().setExternalHost(businessDataTunnelHost);

				LOGGER.info("Set business data tunnel host to: {}", businessDataTunnelHost);
			}
		}
	}

	private void provideConnectivityServiceKeySecretName(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		if (autoProvisionContext.isConnectivityServiceKeySecretNameAutoProvision()) {
			String connectivityServiceKeySecretAutoProvisionName = connectivityProxy.connectivityServiceKeySecretAutoProvisionName();
			connectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService()
					.setSecretName(connectivityServiceKeySecretAutoProvisionName);
			LOGGER.info("Set Connectivity Service Secret name to: {}", connectivityServiceKeySecretAutoProvisionName);
		}
	}

	private void provideConnectivityServiceKeyAttributes(ConnectivityProxy connectivityProxy, AutoProvisionContext autoProvisionContext) {
		if (autoProvisionContext.isSubaccountIdAutoProvision() || autoProvisionContext.isSubaccountSubdomainAutoProvision()) {
			String secretName = connectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService().getSecretName();
			Secret connectivityServiceKeySecret = client.secrets().inNamespace(connectivityProxy.installationNamespace()).withName(secretName).get();

			if (connectivityServiceKeySecret == null) {
				LOGGER.info("Connectivity Service Key Secret {} isn't available", secretName);
				return;
			}

			JsonObject serviceKey = parseServiceKeySecret(connectivityProxy, connectivityServiceKeySecret);

			if (autoProvisionContext.isSubaccountIdAutoProvision()) {
				String extractedSubaccountId = serviceKey.get("subaccount_id").getAsString();
				connectivityProxy.getSpec().getConfig().setSubaccountId(extractedSubaccountId);
				LOGGER.info("Set subaccountId to: {}", extractedSubaccountId);
			}

			if (autoProvisionContext.isSubaccountSubdomainAutoProvision()) {
				String extractedSubaccountSubdomain = serviceKey.get("subaccount_subdomain").getAsString();
				connectivityProxy.getSpec().getConfig().setSubaccountSubdomain(extractedSubaccountSubdomain);
				LOGGER.info("Set subaccount subdomain to: {}", extractedSubaccountSubdomain);
			}
		}
	}

	private JsonObject parseServiceKeySecret(ConnectivityProxy connectivityProxy, Secret connectivityServiceKeySecret) {
		String serviceCredentialsKey = connectivityProxy.getSpec().getConfig().getIntegration().getConnectivityService().getServiceCredentialsKey();

		String serviceKeyEncoded = connectivityServiceKeySecret.getData().get(serviceCredentialsKey);
		byte[] serviceKeyDecodedBytes = Base64.getDecoder().decode(serviceKeyEncoded);
		String serviceKeyDecodedJson = new String(serviceKeyDecodedBytes);

		return GSON.fromJson(serviceKeyDecodedJson, JsonObject.class);
	}

	private void provideIngressTlsSecretName(ConnectivityProxy connectivityProxy) {
		String ingressTlsSecretAutoProvisionName = connectivityProxy.ingressTlsSecretAutoProvisionName();
		connectivityProxy.getSpec().getIngress().getTls().setSecretName(ingressTlsSecretAutoProvisionName);
		LOGGER.info("Set ingress TLS Secret name to: {}", ingressTlsSecretAutoProvisionName);
	}
}
