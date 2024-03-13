package com.sap.cloud.connectivity.operator.core;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

public class ConnectivityProxyStatus extends ObservedGenerationAwareStatus {

	public enum State {
		PROCESSING("Processing"), READY("Ready"), WARNING("Warning"), ERROR("Error"), DELETING("Deleting");

		private final String value;

		State(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@PrinterColumn(name = "STATE")
	private String state;

	public ConnectivityProxyStatus() {
		// Used for Fabric8 Serialization
	}

	public ConnectivityProxyStatus(State state) {
		this.setState(state.getValue());
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
