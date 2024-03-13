package com.sap.cloud.connectivity.operator.core.constants;

import java.util.Map;

public final class ConnectivityProxyConstants {

	private ConnectivityProxyConstants() {
		// Prevent initialization
	}

	public static final String CONNECTIVITY_PROXY_FULL_NAME = "connectivity-proxy";

	public static final String CONNECTIVITY_PROXY_SERVICE_MAPPINGS_VOLUME_NAME = "connectivity-proxy-service-mappings";
	public static final String CONNECTIVITY_PROXY_SERVICE_MAPPINGS_VOLUME_PATH = "/etc/connectivity/properties/servicemappings";
	public static final String CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_SECRETS_TLS_VOLUME_NAME = "connectivity-sm-operator-secrets-tls";
	public static final String CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_SECRETS_TLS_VOLUME_PATH = "/etc/connectivity/secrets/tls";

	public static final int REPLICA_ORDINAL_ZERO = 0;

	public static final String CONNECTIVITY_PROXY_TUNNEL_FULL_NAME = String.format("%s-tunnel", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_PROXY_TUNNEL_SERVICE_ZERO_FULL_NAME = String.format("%s-%s", CONNECTIVITY_PROXY_TUNNEL_FULL_NAME, REPLICA_ORDINAL_ZERO);

	public static final String CONNECTIVITY_PROXY_TUNNEL_PATH = "/connectivity";
	public static final String CONNECTIVITY_PROXY_TUNNEL_PATH_ZERO = String.format("/%s-%s", CONNECTIVITY_PROXY_FULL_NAME, REPLICA_ORDINAL_ZERO);

	public static final String ISTIO_DEFAULT_INSTALLATION_NAMESPACE = "istio-system";
	public static final Map<String, String> ISTIO_INGRESS_GATEWAY_DEFAULT_SELECTOR = Map.of("istio", "ingressgateway");
	public static final Map<String, String> DISABLE_ISTIO_SIDECAR_INJECTION_ANNOTATION = Map.of("sidecar.istio.io/inject", "false");

	public static final String KUBERNETES_SERVICE_FQN_SUFFIX = "svc.cluster.local";
	public static final String TIMEOUTS_SECONDS_SUFFIX = "s";

	public static final int CONNECTIVITY_PROXY_HTTP_SERVER_PORT = 20003;
	public static final int CONNECTIVITY_PROXY_RFC_LDAP_SERVER_PORT = 20001;
	public static final int CONNECTIVITY_PROXY_SOCKS5_SERVER_PORT = 20004;

	public static final int CONNECTIVITY_PROXY_TUNNEL_SERVER_PORT = 8042;

	public static final String CONNECTIVITY_PROXY_RESTART_WATCHER_FULL_NAME = String.format("%s-restart-watcher", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_PROXY_RESTART_WATCHER_LABEL = "connectivityproxy.sap.com/restart";

	public static final String SERVICE_MAPPINGS_CDR_NAME = "servicemappings.connectivityproxy.sap.com";
	public static final String SERVICE_MAPPINGS_CONFIG_MAP_NAME = String.format("%s-service-mappings", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String SERVICE_MAPPINGS_CONFIG_MAP_KEY = "servicemappings";
	public static final String CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME = String.format("%s-sm-validation", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_SERVICE_FULL_NAME = String.format("%s-smv", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_WEBHOOK_FULL_NAME = String.format("%s-webhook", CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME);
	public static final String CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_FULL_NAME = String.format("%s-sm-operator", CONNECTIVITY_PROXY_FULL_NAME);
	public static final int CONNECTIVITY_SERVICE_MAPPINGS_OPERATOR_PORT = 443;

	public static final String CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_FULL_NAME = String.format("%s-region-configurations", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_DATA_KEY = "regionconfigurations";

	public static final String CONNECTIVITY_PROXY_CA_SECRET_NAME = String.format("connectivity-ca-%s", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_PROXY_CA_SECRET_DATA_KEY = "ca.crt";

	public static final String ISTIO_CA_SECRET_NAME_SUFFIX = "cacert";
	public static final String ISTIO_CA_SECRET_DATA_KEY = "cacert";

	public static final String CONNECTIVITY_PROXY_CONFIG_VOLUME_NAME = String.format("%s-config", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_PROXY_CONFIG_MOUNT_PATH = "/etc/connectivity/properties/user-provided";

	public static final String CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_VOLUME_NAME = String.format("%s-region-configurations", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_PROXY_REGION_CONFIGURATIONS_SECRET_MOUNT_PATH = "/etc/connectivity/secrets/regionconfigurations";

	public static final String CONNECTIVITY_PROXY_CA_SECRET_VOLUME_NAME = String.format("%s-ca", CONNECTIVITY_PROXY_FULL_NAME);
	public static final String CONNECTIVITY_PROXY_CA_SECRET_MOUNT_PATH = "/etc/connectivity/secrets/ca";
}
