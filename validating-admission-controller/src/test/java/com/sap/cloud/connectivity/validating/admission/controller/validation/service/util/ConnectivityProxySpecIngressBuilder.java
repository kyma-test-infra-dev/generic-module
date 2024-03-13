package com.sap.cloud.connectivity.validating.admission.controller.validation.service.util;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec;

public class ConnectivityProxySpecIngressBuilder {

	private String className;

	private String tlsSecretName;

	public ConnectivityProxySpec.Ingress build() {
		ConnectivityProxySpec.Ingress ingress = new ConnectivityProxySpec.Ingress();
		ingress.setClassName(className);
		ingress.setTls(createTls());

		return ingress;
	}

	private ConnectivityProxySpec.Ingress.Tls createTls() {
		ConnectivityProxySpec.Ingress.Tls tls = new ConnectivityProxySpec.Ingress.Tls();
		tls.setSecretName(tlsSecretName);
		return tls;
	}

	public ConnectivityProxySpecIngressBuilder className(String className) {
		this.className = className;
		return this;
	}

	public ConnectivityProxySpecIngressBuilder tlsSecretName(String tlsSecretName) {
		this.tlsSecretName = tlsSecretName;
		return this;
	}
}
