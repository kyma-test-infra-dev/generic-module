package com.sap.cloud.connectivity.proxy.operator.provisioning;

import static com.sap.cloud.connectivity.proxy.operator.constants.OperatorConstants.AUTO_PROVISION;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;

public final class AutoProvisionContext {

	private final boolean isSubaccountIdAutoProvision;
	private final boolean isSubaccountSubdomainAutoProvision;
	private final boolean isConnectivityServiceKeySecretNameAutoProvision;
	private final boolean isIngressTlsSecretNameAutoProvision;
	private final boolean isExternalBusinessHostAutoProvision;

	public AutoProvisionContext(ConnectivityProxy connectivityProxy) {
		String subaccountId = connectivityProxy.getSpec().getConfig().getSubaccountId();
		this.isSubaccountIdAutoProvision = AUTO_PROVISION.equalsIgnoreCase(subaccountId);

		String subaccountSubdomain = connectivityProxy.getSpec().getConfig().getSubaccountSubdomain();
		this.isSubaccountSubdomainAutoProvision = AUTO_PROVISION.equalsIgnoreCase(subaccountSubdomain);

		String connectivityServiceKeySecretName = connectivityProxy.getSpec().getSecretConfig().getIntegration().getConnectivityService()
				.getSecretName();
		this.isConnectivityServiceKeySecretNameAutoProvision = AUTO_PROVISION.equalsIgnoreCase(connectivityServiceKeySecretName);

		String ingressTlsSecretName = connectivityProxy.getSpec().getIngress().getTls().getSecretName();
		this.isIngressTlsSecretNameAutoProvision = AUTO_PROVISION.equalsIgnoreCase(ingressTlsSecretName);

		String externalHost = connectivityProxy.getSpec().getConfig().getServers().getBusinessDataTunnel().getExternalHost();
		this.isExternalBusinessHostAutoProvision = AUTO_PROVISION.equalsIgnoreCase(externalHost);
	}

	public boolean isSubaccountIdAutoProvision() {
		return isSubaccountIdAutoProvision;
	}

	public boolean isSubaccountSubdomainAutoProvision() {
		return isSubaccountSubdomainAutoProvision;
	}

	public boolean isConnectivityServiceKeySecretNameAutoProvision() {
		return isConnectivityServiceKeySecretNameAutoProvision;
	}

	public boolean isExternalBusinessHostAutoProvision() {
		return isExternalBusinessHostAutoProvision;
	}

	public boolean isIngressTlsSecretNameAutoProvision() {
		return isIngressTlsSecretNameAutoProvision;
	}

}
