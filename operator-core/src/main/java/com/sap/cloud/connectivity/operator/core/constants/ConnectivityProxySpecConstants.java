package com.sap.cloud.connectivity.operator.core.constants;

public final class ConnectivityProxySpecConstants {

	private ConnectivityProxySpecConstants() {
		// Prevent initialization
	}

	public static final String TENANT_PATTERN = "^(dedicated|shared)$";

	public static final String PULL_POLICY_PATTERN = "^(Always|IfNotPresent|Never)$";

	public static final String AUDIT_LOG_MODE_PATTERN = "^(console|service)$";

	public static final String SUBACCOUNT_ID_PATTERN = "^[a-zA-Z0-9-]+$";

	public static final String BASE64_ENCODED_STRING_PATTERN = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$";

	public static final String INGRESS_CLASSNAME_PATTERN = "^(nginx|istio)$";

	public static final String HIGH_AVAILABILITY_MODE_PATTERN = "^(off|path|subdomain)$";

	public static final int MIN_PORT = 1;
	public static final int MAX_PORT = 65535;

	public static final int MIN_REPLICA_COUNT = 1;

	public static final int MAX_FILE_DESCRIPTOR_COUNT_MIN_LIMIT = 10;
}
