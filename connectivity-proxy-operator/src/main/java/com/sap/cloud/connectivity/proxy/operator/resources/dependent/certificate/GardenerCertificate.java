package com.sap.cloud.connectivity.proxy.operator.resources.dependent.certificate;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("cert.gardener.cloud")
@Version("v1alpha1")
@Kind("Certificate")
public class GardenerCertificate extends CustomResource<GardenerCertificateSpec, Void> implements Namespaced {
}
