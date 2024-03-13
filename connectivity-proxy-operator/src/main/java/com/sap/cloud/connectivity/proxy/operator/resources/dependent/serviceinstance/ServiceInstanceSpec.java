package com.sap.cloud.connectivity.proxy.operator.resources.dependent.serviceinstance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceInstanceSpec {

	private String serviceOfferingName;
	private String servicePlanName;
	private String externalName;

	public String getServiceOfferingName() {
		return serviceOfferingName;
	}

	public void setServiceOfferingName(String serviceOfferingName) {
		this.serviceOfferingName = serviceOfferingName;
	}

	public String getServicePlanName() {
		return servicePlanName;
	}

	public void setServicePlanName(String servicePlanName) {
		this.servicePlanName = servicePlanName;
	}

	public String getExternalName() {
		return externalName;
	}

	public void setExternalName(String externalName) {
		this.externalName = externalName;
	}
}
