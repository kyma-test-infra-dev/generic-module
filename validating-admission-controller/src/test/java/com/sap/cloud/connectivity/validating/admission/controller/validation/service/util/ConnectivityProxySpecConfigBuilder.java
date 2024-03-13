package com.sap.cloud.connectivity.validating.admission.controller.validation.service.util;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec.Config.Servers.Proxy;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec.Config.Servers.Proxy.Authorization;
import com.sap.cloud.connectivity.operator.core.ConnectivityProxySpec.Config.Servers.Proxy.Authorization.oAuth;
import com.sap.cloud.connectivity.validating.admission.controller.validation.service.ConnectivityProxyValidator;

/**
 * A utility class, which can be used to set the values of the "config" section of the ConnectivityProxy custom
 * resources. It initializes some of them with a default value, according the current default "values.yaml".
 */
public class ConnectivityProxySpecConfigBuilder {

	private boolean rfcProxyEnabled = false;
	private boolean rfcAndLdapProxyAuthorization = true;

	private boolean httpProxyEnabled = false;
	private boolean httpProxyAuthorization = true;

	private boolean socks5ProxyEnabled = true;
	private boolean socks5ProxyAuthorization = true;

	private String tenantMode = ConnectivityProxyValidator.TenantMode.DEDICATED.getMode();

	private boolean serviceChannelsEnabled = true;

	private boolean multiRegionModeEnabled = false;
	private String multiRegionModeConfigMapName = null;

	private String subaccountId;

	private String subaccountSubdomain;

	private String allowedClientId;

	public ConnectivityProxySpec.Config build() {
		ConnectivityProxySpec.Config config = new ConnectivityProxySpec.Config();

		config.setServers(createServers());
		config.setTenantMode(tenantMode);
		config.setSubaccountId(subaccountId);
		config.setSubaccountSubdomain(subaccountSubdomain);
		config.setServiceChannels(createServiceChannels());
		config.setMultiRegionMode(createMultiRegionMode());

		return config;
	}

	private ConnectivityProxySpec.Config.Servers createServers() {
		ConnectivityProxySpec.Config.Servers servers = new ConnectivityProxySpec.Config.Servers();
		servers.setProxy(createProxyProperty());
		return servers;
	}

	private Proxy createProxyProperty() {
		Proxy proxy = new Proxy();

		proxy.setRfcAndLdap(createRfcAndLdapProxyServer());
		proxy.setHttp(createHttpProxyServer());
		proxy.setSocks5(createSocks5ProxyServer());

		if (allowedClientId != null) {
			proxy.setAuthorization(createAuthorizationProperty());
		}
		return proxy;
	}

	private Proxy.RfcAndLdap createRfcAndLdapProxyServer() {
		Proxy.RfcAndLdap rfcAndLdap = new Proxy.RfcAndLdap();
		rfcAndLdap.setEnabled(rfcProxyEnabled);
		rfcAndLdap.setEnableProxyAuthorization(rfcAndLdapProxyAuthorization);
		return rfcAndLdap;
	}

	private Proxy.Http createHttpProxyServer() {
		Proxy.Http http = new Proxy.Http();
		http.setEnabled(httpProxyEnabled);
		http.setEnableProxyAuthorization(httpProxyAuthorization);
		return http;
	}

	private Proxy.Socks5 createSocks5ProxyServer() {
		Proxy.Socks5 socks5 = new Proxy.Socks5();
		socks5.setEnabled(socks5ProxyEnabled);
		socks5.setEnableProxyAuthorization(socks5ProxyAuthorization);
		return socks5;
	}

	private Authorization createAuthorizationProperty() {
		oAuth oAuth = new oAuth();
		oAuth.setAllowedClientId(allowedClientId);

		Authorization authorization = new Authorization();
		authorization.setOauth(oAuth);

		return authorization;
	}

	public ConnectivityProxySpecConfigBuilder rfcProxyEnabled(boolean rfcProxyEnabled) {
		this.rfcProxyEnabled = rfcProxyEnabled;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder rfcProxyAuthorization(boolean rfcProxyAuthorization) {
		this.rfcAndLdapProxyAuthorization = rfcProxyAuthorization;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder httpProxyEnabled(boolean httpProxyEnabled) {
		this.httpProxyEnabled = httpProxyEnabled;
		return this;

	}

	public ConnectivityProxySpecConfigBuilder httpProxyAuthorization(boolean httpProxyAuthorization) {
		this.httpProxyAuthorization = httpProxyAuthorization;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder socks5ProxyEnabled(boolean socks5ProxyEnabled) {
		this.socks5ProxyEnabled = socks5ProxyEnabled;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder socks5ProxyAuthorization(boolean socks5ProxyAuthorization) {
		this.socks5ProxyAuthorization = socks5ProxyAuthorization;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder setProxyServersAuthorization(boolean rfcProxyAuthorization, boolean httpProxyAuthorization,
			boolean socks5ProxyAuthorization) {
		rfcProxyAuthorization(rfcProxyAuthorization);
		httpProxyAuthorization(httpProxyAuthorization);
		socks5ProxyAuthorization(socks5ProxyAuthorization);
		return this;
	}

	public ConnectivityProxySpecConfigBuilder setProxyServersEnabled(boolean rfcProxyEnabled, boolean httpProxyEnabled, boolean socks5ProxyEnabled) {
		rfcProxyEnabled(rfcProxyEnabled);
		httpProxyEnabled(httpProxyEnabled);
		socks5ProxyEnabled(socks5ProxyEnabled);
		return this;
	}

	public ConnectivityProxySpecConfigBuilder tenantMode(ConnectivityProxyValidator.TenantMode tenantMode) {
		this.tenantMode = tenantMode.getMode();
		return this;
	}

	public ConnectivityProxySpecConfigBuilder subaccountId(String subaccountId) {
		this.subaccountId = subaccountId;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder subaccountSubdomain(String subaccountSubdomain) {
		this.subaccountSubdomain = subaccountSubdomain;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder allowedClientId(String allowedClientId) {
		this.allowedClientId = allowedClientId;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder setServiceChannelsEnabled(boolean serviceChannelsEnabled) {
		this.serviceChannelsEnabled = serviceChannelsEnabled;
		return this;
	}

	private ConnectivityProxySpec.Config.ServiceChannels createServiceChannels() {
		ConnectivityProxySpec.Config.ServiceChannels serviceChannels = new ConnectivityProxySpec.Config.ServiceChannels();
		serviceChannels.setEnabled(serviceChannelsEnabled);
		return serviceChannels;
	}

	public ConnectivityProxySpecConfigBuilder setMultiRegionModeEnabled(boolean multiRegionModeEnabled) {
		this.multiRegionModeEnabled = multiRegionModeEnabled;
		return this;
	}

	public ConnectivityProxySpecConfigBuilder setMultiRegionModeConfigMapName(String multiRegionModeConfigMapName) {
		this.multiRegionModeConfigMapName = multiRegionModeConfigMapName;
		return this;
	}

	private ConnectivityProxySpec.Config.MultiRegionMode createMultiRegionMode() {
		ConnectivityProxySpec.Config.MultiRegionMode multiRegionMode = new ConnectivityProxySpec.Config.MultiRegionMode();
		multiRegionMode.setEnabled(multiRegionModeEnabled);
		multiRegionMode.setConfigMapName(multiRegionModeConfigMapName);
		return multiRegionMode;
	}
}
