package com.sap.cloud.connectivity.proxy.operator;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sap.cloud.connectivity.proxy.operator.probes.LivenessHandler;
import com.sun.net.httpserver.HttpServer;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;

public class ConnectivityProxyOperator {

	private static final Logger LOGGER = LogManager.getLogger(ConnectivityProxyOperator.class);

	private static final int LIVENESS_SERVER_PORT = 8080;
	private static final String CRD_RESOURCE_NOT_FOUND_ERROR_MESSAGE = "Required %s CRD resources aren't available on the cluster, won't start the Operator!";

	public static void main(String[] args) {
		try {
			KubernetesClient kubernetesClient = new KubernetesClientBuilder().build();

			assertRequiredCRDResourcesArePresent(kubernetesClient);

			LOGGER.info("Starting Connectivity Proxy operator in namespace {}", kubernetesClient.getNamespace());
			Operator operator = new Operator(configurationServiceOverrider -> configurationServiceOverrider.withKubernetesClient(kubernetesClient));
			operator.register(new ConnectivityProxyReconciler(kubernetesClient));
			operator.start();

			LOGGER.info("Starting Liveness Server on port {}", LIVENESS_SERVER_PORT);
			HttpServer server = HttpServer.create(new InetSocketAddress(LIVENESS_SERVER_PORT), 0);
			server.createContext("/health", new LivenessHandler(operator));
			server.start();
		} catch (Exception e) {
			LOGGER.error("Failed to start Operator", e);
			System.exit(1);
		}
	}

	private static void assertRequiredCRDResourcesArePresent(KubernetesClient kubernetesClient) throws ConnectivityProxyOperatorStartupException {
		List<CustomResourceDefinition> crdsFromCluster = kubernetesClient.apiextensions().v1().customResourceDefinitions().list().getItems();
		Set<String> crdNamesFromCluster = crdsFromCluster.stream().map(crd -> crd.getMetadata().getName()).collect(Collectors.toSet());

		Set<String> connectivityProxyCRDName = Set.of("connectivityproxies.connectivityproxy.sap.com");
		if (!crdNamesFromCluster.containsAll(connectivityProxyCRDName)) {
			throw new ConnectivityProxyOperatorStartupException(String.format(CRD_RESOURCE_NOT_FOUND_ERROR_MESSAGE, "Connectivity Proxy"));
		}

		Set<String> istioCRDNames = Set.of("gateways.networking.istio.io", "virtualservices.networking.istio.io",
				"destinationrules.networking.istio.io", "envoyfilters.networking.istio.io", "peerauthentications.security.istio.io");
		if (!crdNamesFromCluster.containsAll(istioCRDNames)) {
			throw new ConnectivityProxyOperatorStartupException(String.format(CRD_RESOURCE_NOT_FOUND_ERROR_MESSAGE, "Istio"));
		}

		Set<String> gardenerCRDNames = Set.of("certificates.cert.gardener.cloud");
		if (!crdNamesFromCluster.containsAll(gardenerCRDNames)) {
			throw new ConnectivityProxyOperatorStartupException(String.format(CRD_RESOURCE_NOT_FOUND_ERROR_MESSAGE, "Gardener"));
		}

		Set<String> btpOperatorCRDNames = Set.of("serviceinstances.services.cloud.sap.com", "servicebindings.services.cloud.sap.com");
		if (!crdNamesFromCluster.containsAll(btpOperatorCRDNames)) {
			throw new ConnectivityProxyOperatorStartupException(String.format(CRD_RESOURCE_NOT_FOUND_ERROR_MESSAGE, "BTP Operator"));
		}

		LOGGER.info("Required CRD resources are present in the cluster");
	}

}
