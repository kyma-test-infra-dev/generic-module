package com.sap.cloud.connectivity.operator.core;

import java.util.List;
import java.util.Map;

import com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxySpecConstants;

import io.fabric8.generator.annotation.Max;
import io.fabric8.generator.annotation.Min;
import io.fabric8.generator.annotation.Pattern;
import io.fabric8.generator.annotation.Required;

public class ConnectivityProxySpec {

	@Required
	private Config config;

	@Required
	private Deployment deployment;

	@Required
	private Ingress ingress;

	@Required
	private SecretConfig secretConfig;

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public Deployment getDeployment() {
		return deployment;
	}

	public void setDeployment(Deployment deployment) {
		this.deployment = deployment;
	}

	public Ingress getIngress() {
		return ingress;
	}

	public void setIngress(Ingress ingress) {
		this.ingress = ingress;
	}

	public SecretConfig getSecretConfig() {
		return secretConfig;
	}

	public void setSecretConfig(SecretConfig secretConfig) {
		this.secretConfig = secretConfig;
	}

	public static class Config {

		@Required
		private Integration integration;

		@Required
		private Servers servers;

		@Pattern(ConnectivityProxySpecConstants.SUBACCOUNT_ID_PATTERN)
		private String subaccountId;

		private String subaccountSubdomain;

		@Required
		@Pattern(ConnectivityProxySpecConstants.TENANT_PATTERN)
		private String tenantMode;

		private ServiceChannels serviceChannels;

		private MultiRegionMode multiRegionMode;

		@Pattern(ConnectivityProxySpecConstants.HIGH_AVAILABILITY_MODE_PATTERN)
		private String highAvailabilityMode;

		public Integration getIntegration() {
			return integration;
		}

		public void setIntegration(Integration integration) {
			this.integration = integration;
		}

		public Servers getServers() {
			return servers;
		}

		public void setServers(Servers servers) {
			this.servers = servers;
		}

		public String getSubaccountId() {
			return subaccountId;
		}

		public void setSubaccountId(String subaccountId) {
			this.subaccountId = subaccountId;
		}

		public String getSubaccountSubdomain() {
			return subaccountSubdomain;
		}

		public void setSubaccountSubdomain(String subaccountSubdomain) {
			this.subaccountSubdomain = subaccountSubdomain;
		}

		public String getTenantMode() {
			return tenantMode;
		}

		public void setTenantMode(String tenantMode) {
			this.tenantMode = tenantMode;
		}

		public ServiceChannels getServiceChannels() {
			return serviceChannels;
		}

		public void setServiceChannels(ServiceChannels serviceChannels) {
			this.serviceChannels = serviceChannels;
		}

		public MultiRegionMode getMultiRegionMode() {
			return multiRegionMode;
		}

		public void setMultiRegionMode(MultiRegionMode multiRegionMode) {
			this.multiRegionMode = multiRegionMode;
		}

		public String getHighAvailabilityMode() {
			return highAvailabilityMode;
		}

		public void setHighAvailabilityMode(String highAvailabilityMode) {
			this.highAvailabilityMode = highAvailabilityMode;
		}

		public static class Integration {

			@Required
			private AuditLog auditlog;

			@Required
			private ConnectivityService connectivityService;

			public AuditLog getAuditlog() {
				return auditlog;
			}

			public void setAuditlog(AuditLog auditlog) {
				this.auditlog = auditlog;
			}

			public ConnectivityService getConnectivityService() {
				return connectivityService;
			}

			public void setConnectivityService(ConnectivityService connectivityService) {
				this.connectivityService = connectivityService;
			}

			public static class AuditLog {

				@Required
				@Pattern(ConnectivityProxySpecConstants.AUDIT_LOG_MODE_PATTERN)
				private String mode;

				private String serviceCredentialsKey;

				public String getMode() {
					return mode;
				}

				public void setMode(String mode) {
					this.mode = mode;
				}

				public String getServiceCredentialsKey() {
					return serviceCredentialsKey;
				}

				public void setServiceCredentialsKey(String serviceCredentialsKey) {
					this.serviceCredentialsKey = serviceCredentialsKey;
				}
			}

			public static class ConnectivityService {

				@Required
				private String serviceCredentialsKey;

				public String getServiceCredentialsKey() {
					return serviceCredentialsKey;
				}

				public void setServiceCredentialsKey(String serviceCredentialsKey) {
					this.serviceCredentialsKey = serviceCredentialsKey;
				}
			}
		}

		public static class Servers {

			@Required
			private BusinessDataTunnel businessDataTunnel;

			@Required
			private Proxy proxy;

			public BusinessDataTunnel getBusinessDataTunnel() {
				return businessDataTunnel;
			}

			public void setBusinessDataTunnel(BusinessDataTunnel businessDataTunnel) {
				this.businessDataTunnel = businessDataTunnel;
			}

			public Proxy getProxy() {
				return proxy;
			}

			public void setProxy(Proxy proxy) {
				this.proxy = proxy;
			}

			public static class BusinessDataTunnel {

				@Required
				private String externalHost;

				@Required
				@Min(ConnectivityProxySpecConstants.MIN_PORT)
				@Max(ConnectivityProxySpecConstants.MAX_PORT)
				private Integer externalPort;

				public String getExternalHost() {
					return externalHost;
				}

				public void setExternalHost(String externalHost) {
					this.externalHost = externalHost;
				}

				public Integer getExternalPort() {
					return externalPort;
				}

				public void setExternalPort(Integer externalPort) {
					this.externalPort = externalPort;
				}
			}

			public static class Proxy {

				private Authorization authorization;

				private Http http;

				private RfcAndLdap rfcAndLdap;

				private Socks5 socks5;

				public Authorization getAuthorization() {
					return authorization;
				}

				public void setAuthorization(Authorization authorization) {
					this.authorization = authorization;
				}

				public Http getHttp() {
					return http;
				}

				public void setHttp(Http http) {
					this.http = http;
				}

				public RfcAndLdap getRfcAndLdap() {
					return rfcAndLdap;
				}

				public void setRfcAndLdap(RfcAndLdap rfcAndLdap) {
					this.rfcAndLdap = rfcAndLdap;
				}

				public Socks5 getSocks5() {
					return socks5;
				}

				public void setSocks5(Socks5 socks5) {
					this.socks5 = socks5;
				}

				public static class Authorization {

					@Required
					private oAuth oauth;

					public oAuth getOauth() {
						return oauth;
					}

					public void setOauth(oAuth oauth) {
						this.oauth = oauth;
					}

					public static class oAuth {

						@Required
						private String allowedClientId;

						public String getAllowedClientId() {
							return allowedClientId;
						}

						public void setAllowedClientId(String allowedClientId) {
							this.allowedClientId = allowedClientId;
						}
					}
				}

				public static class Http {

					private Boolean enabled;

					private Boolean allowRemoteConnections;

					private Boolean enableProxyAuthorization;

					@Min(ConnectivityProxySpecConstants.MIN_PORT)
					@Max(ConnectivityProxySpecConstants.MAX_PORT)
					private Integer port;

					public Boolean getEnabled() {
						return enabled;
					}

					public void setEnabled(Boolean enabled) {
						this.enabled = enabled;
					}

					public Boolean getAllowRemoteConnections() {
						return allowRemoteConnections;
					}

					public void setAllowRemoteConnections(Boolean allowRemoteConnections) {
						this.allowRemoteConnections = allowRemoteConnections;
					}

					public Boolean getEnableProxyAuthorization() {
						return enableProxyAuthorization;
					}

					public void setEnableProxyAuthorization(Boolean enableProxyAuthorization) {
						this.enableProxyAuthorization = enableProxyAuthorization;
					}

					public Integer getPort() {
						return port;
					}

					public void setPort(Integer port) {
						this.port = port;
					}
				}

				public static class RfcAndLdap {

					private Boolean enabled;

					private Boolean allowRemoteConnections;

					private Boolean enableProxyAuthorization;

					@Min(ConnectivityProxySpecConstants.MIN_PORT)
					@Max(ConnectivityProxySpecConstants.MAX_PORT)
					private Integer port;

					public Boolean getEnabled() {
						return enabled;
					}

					public void setEnabled(Boolean enabled) {
						this.enabled = enabled;
					}

					public Boolean getAllowRemoteConnections() {
						return allowRemoteConnections;
					}

					public void setAllowRemoteConnections(Boolean allowRemoteConnections) {
						this.allowRemoteConnections = allowRemoteConnections;
					}

					public Boolean getEnableProxyAuthorization() {
						return enableProxyAuthorization;
					}

					public void setEnableProxyAuthorization(Boolean enableProxyAuthorization) {
						this.enableProxyAuthorization = enableProxyAuthorization;
					}

					public Integer getPort() {
						return port;
					}

					public void setPort(Integer port) {
						this.port = port;
					}
				}

				public static class Socks5 {

					private Boolean enabled;

					private Boolean allowRemoteConnections;

					private Boolean enableProxyAuthorization;

					@Min(ConnectivityProxySpecConstants.MIN_PORT)
					@Max(ConnectivityProxySpecConstants.MAX_PORT)
					private Integer port;

					public Boolean getEnabled() {
						return enabled;
					}

					public void setEnabled(Boolean enabled) {
						this.enabled = enabled;
					}

					public Boolean getAllowRemoteConnections() {
						return allowRemoteConnections;
					}

					public void setAllowRemoteConnections(Boolean allowRemoteConnections) {
						this.allowRemoteConnections = allowRemoteConnections;
					}

					public Boolean getEnableProxyAuthorization() {
						return enableProxyAuthorization;
					}

					public void setEnableProxyAuthorization(Boolean enableProxyAuthorization) {
						this.enableProxyAuthorization = enableProxyAuthorization;
					}

					public Integer getPort() {
						return port;
					}

					public void setPort(Integer port) {
						this.port = port;
					}
				}
			}
		}

		public static class ServiceChannels {

			@Required
			private Boolean enabled;

			public Boolean getEnabled() {
				return enabled;
			}

			public void setEnabled(Boolean enabled) {
				this.enabled = enabled;
			}
		}

		public static class MultiRegionMode {

			@Required
			private Boolean enabled;

			private String configMapName;

			public Boolean getEnabled() {
				return enabled;
			}

			public void setEnabled(Boolean enabled) {
				this.enabled = enabled;
			}

			public String getConfigMapName() {
				return configMapName;
			}

			public void setConfigMapName(String configMapName) {
				this.configMapName = configMapName;
			}
		}
	}

	public static class Deployment {

		@Required
		private Image image;

		@Required
		private Image utilityImage;

		@Required
		@Min(ConnectivityProxySpecConstants.MIN_REPLICA_COUNT)
		private int replicaCount;

		@Required
		private Resources resources;

		private Map<String, String> nodeSelector;

		private RestartWatcher restartWatcher;

		public Image getImage() {
			return image;
		}

		public void setImage(Image image) {
			this.image = image;
		}

		public Image getUtilityImage() {
			return utilityImage;
		}

		public void setUtilityImage(Image utilityImage) {
			this.utilityImage = utilityImage;
		}

		public int getReplicaCount() {
			return replicaCount;
		}

		public void setReplicaCount(int replicaCount) {
			this.replicaCount = replicaCount;
		}

		public Resources getResources() {
			return resources;
		}

		public void setResources(Resources resources) {
			this.resources = resources;
		}

		public RestartWatcher getRestartWatcher() {
			return restartWatcher;
		}

		public void setRestartWatcher(RestartWatcher restartWatcher) {
			this.restartWatcher = restartWatcher;
		}

		public Map<String, String> getNodeSelector() {
			return nodeSelector;
		}

		public void setNodeSelector(Map<String, String> nodeSelector) {
			this.nodeSelector = nodeSelector;
		}

		public static class Image {

			@Required
			private String registry;

			@Required
			private String repository;

			@Required
			private String tag;

			private String digest;

			@Pattern(ConnectivityProxySpecConstants.PULL_POLICY_PATTERN)
			private String pullPolicy;

			private String pullSecret;

			public String getRegistry() {
				return registry;
			}

			public void setRegistry(String registry) {
				this.registry = registry;
			}

			public String getRepository() {
				return repository;
			}

			public void setRepository(String repository) {
				this.repository = repository;
			}

			public String getTag() {
				return tag;
			}

			public void setTag(String tag) {
				this.tag = tag;
			}

			public String getDigest() {
				return digest;
			}

			public void setDigest(String digest) {
				this.digest = digest;
			}

			public String getPullPolicy() {
				return pullPolicy;
			}

			public void setPullPolicy(String pullPolicy) {
				this.pullPolicy = pullPolicy;
			}

			public String getPullSecret() {
				return pullSecret;
			}

			public void setPullSecret(String pullSecret) {
				this.pullSecret = pullSecret;
			}
		}

		public static class Resources {

			private Requests requests;

			@Required
			@Min(ConnectivityProxySpecConstants.MAX_FILE_DESCRIPTOR_COUNT_MIN_LIMIT)
			private Integer maxFileDescriptorCount;

			@Required
			private Limits limits;

			public Requests getRequests() {
				return requests;
			}

			public void setRequests(Requests requests) {
				this.requests = requests;
			}

			public Integer getMaxFileDescriptorCount() {
				return maxFileDescriptorCount;
			}

			public void setMaxFileDescriptorCount(Integer maxFileDescriptorCount) {
				this.maxFileDescriptorCount = maxFileDescriptorCount;
			}

			public Limits getLimits() {
				return limits;
			}

			public void setLimits(Limits limits) {
				this.limits = limits;
			}

			public static class Requests {

				@Required
				private String cpu;

				@Required
				private String memory;

				public String getCPU() {
					return cpu;
				}

				public void setCpu(String cpu) {
					this.cpu = cpu;
				}

				public String getMemory() {
					return memory;
				}

				public void setMemory(String memory) {
					this.memory = memory;
				}
			}

			public static class Limits {

				@Required
				private String cpu;

				@Required
				private String memory;

				public String getCPU() {
					return cpu;
				}

				public void setCpu(String cpu) {
					this.cpu = cpu;
				}

				public String getMemory() {
					return memory;
				}

				public void setMemory(String memory) {
					this.memory = memory;
				}
			}
		}

		public static class RestartWatcher {

			@Required
			private Boolean enabled;

			public Boolean getEnabled() {
				return enabled;
			}

			public void setEnabled(Boolean enabled) {
				this.enabled = enabled;
			}
		}
	}

	public static class Ingress {

		@Pattern(ConnectivityProxySpecConstants.INGRESS_CLASSNAME_PATTERN)
		private String className;

		private Tls tls;

		@Required
		private Timeouts timeouts;

		private Istio istio;

		private Map<String, String> annotations;

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public Tls getTls() {
			return tls;
		}

		public void setTls(Tls tls) {
			this.tls = tls;
		}

		public Timeouts getTimeouts() {
			return timeouts;
		}

		public void setTimeouts(Timeouts timeouts) {
			this.timeouts = timeouts;
		}

		public Istio getIstio() {
			return istio;
		}

		public void setIstio(Istio istio) {
			this.istio = istio;
		}

		public Map<String, String> getAnnotations() {
			return annotations;
		}

		public void setAnnotations(Map<String, String> annotations) {
			this.annotations = annotations;
		}

		public static class Tls {

			@Required
			private String secretName;

			public String getSecretName() {
				return secretName;
			}

			public void setSecretName(String secretName) {
				this.secretName = secretName;
			}
		}

		public static class Timeouts {

			private Proxy proxy;

			public Proxy getProxy() {
				return proxy;
			}

			public void setProxy(Proxy proxy) {
				this.proxy = proxy;
			}

			public static class Proxy {

				@Required
				private Integer connect;

				@Required
				private Integer read;

				@Required
				private Integer send;

				public Integer getConnect() {
					return connect;
				}

				public void setConnect(Integer connect) {
					this.connect = connect;
				}

				public Integer getRead() {
					return read;
				}

				public void setRead(Integer read) {
					this.read = read;
				}

				public Integer getSend() {
					return send;
				}

				public void setSend(Integer send) {
					this.send = send;
				}
			}
		}

		public static class Istio {

			private String namespace;

			private Gateway gateway;

			private Tls tls;

			public String getNamespace() {
				return namespace;
			}

			public void setNamespace(String namespace) {
				this.namespace = namespace;
			}

			public Gateway getGateway() {
				return gateway;
			}

			public void setGateway(Gateway gateway) {
				this.gateway = gateway;
			}

			public Tls getTls() {
				return tls;
			}

			public void setTls(Tls tls) {
				this.tls = tls;
			}

			public static class Gateway {

				private Map<String, String> selector;

				public Map<String, String> getSelector() {
					return selector;
				}

				public void setSelector(Map<String, String> selector) {
					this.selector = selector;
				}
			}

			public static class Tls {
				private List<String> ciphers;

				public List<String> getCiphers() {
					return ciphers;
				}

				public void setCiphers(List<String> ciphers) {
					this.ciphers = ciphers;
				}
			}
		}
	}

	public static class SecretConfig {

		@Required
		private Integration integration;

		public Integration getIntegration() {
			return integration;
		}

		public void setIntegration(Integration integration) {
			this.integration = integration;
		}

		public static class Integration {

			@Required
			private ConnectivityService connectivityService;

			private AuditlogService auditlogService;

			public ConnectivityService getConnectivityService() {
				return connectivityService;
			}

			public void setConnectivityService(ConnectivityService connectivityService) {
				this.connectivityService = connectivityService;
			}

			public AuditlogService getAuditlogService() {
				return auditlogService;
			}

			public void setAuditlogService(AuditlogService auditlogService) {
				this.auditlogService = auditlogService;
			}

			public static class ConnectivityService {

				@Required
				private String secretName;

				@Pattern(ConnectivityProxySpecConstants.BASE64_ENCODED_STRING_PATTERN)
				private String secretData;

				public String getSecretName() {
					return secretName;
				}

				public void setSecretName(String secretName) {
					this.secretName = secretName;
				}

				public String getSecretData() {
					return secretData;
				}

				public void setSecretData(String secretData) {
					this.secretData = secretData;
				}
			}

			public static class AuditlogService {

				@Required
				private String secretName;

				@Pattern(ConnectivityProxySpecConstants.BASE64_ENCODED_STRING_PATTERN)
				private String secretData;

				public String getSecretName() {
					return secretName;
				}

				public void setSecretName(String secretName) {
					this.secretName = secretName;
				}

				public String getSecretData() {
					return secretData;
				}

				public void setSecretData(String secretData) {
					this.secretData = secretData;
				}
			}
		}
	}

}
