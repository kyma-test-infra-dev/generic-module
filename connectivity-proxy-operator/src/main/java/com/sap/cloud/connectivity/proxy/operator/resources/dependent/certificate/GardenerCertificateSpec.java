package com.sap.cloud.connectivity.proxy.operator.resources.dependent.certificate;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GardenerCertificateSpec {

	private String commonName;

	private List<String> dnsNames;

	private String secretName;

	public String getCommonName() {
		return commonName;
	}

	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}

	public List<String> getDnsNames() {
		return dnsNames;
	}

	public void setDnsNames(List<String> dnsNames) {
		this.dnsNames = dnsNames;
	}

	public String getSecretName() {
		return secretName;
	}

	public void setSecretName(String secretName) {
		this.secretName = secretName;
	}
}
