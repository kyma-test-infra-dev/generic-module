package com.sap.cloud.connectivity.proxy.operator.resources.dependent.serviceinstance;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("services.cloud.sap.com")
@Version("v1")
@Kind("ServiceInstance")
public class ServiceInstance extends CustomResource<ServiceInstanceSpec, Void> implements Namespaced {
}
