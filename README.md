# Connectivity Service K8s Operator

## Release to Kyma 
1. Release Operator image
   1. Bump the version in [cfg/xmake.cfg](./cfg/xmake.cfg)
   2. Trigger the [Jenkins release job](https://xmake-dev.wdf.sap.corp/job/scp-cf/job/scp-cf-connectivity-service-k8s-operator-SP-MS-common/)
   3. Wait until the image is created in [SAP artifactory](https://int.repositories.cloud.sap/ui/packages/docker:%2F%2Fcom.sap.cloud.connectivity%2Fconnectivity-proxy-operator?name=connectivity-proxy-operator&type=packages). As that could take some time, execute *docker pull docker.wdf.sap.corp:51021/com.sap.cloud.connectivity/connectivity-proxy-operator:x.x.x* to check if the image is available
2. Make the image available in external repository
   1. Contact Tsvetan in order to upload the newly released image to an external image repository: *"europe-docker.pkg.dev/kyma-project/prod/connectivity-proxy/operator:tag"*
3. Make a change in Kyma module manifests project
   1. Checkout the [module manifest repository](https://github.tools.sap/kyma/module-manifests) and create a branch for the change
   2. Choose a channel from [the directory for Connectivity Proxy module](https://github.tools.sap/kyma/module-manifests/tree/main/modules/connectivity-proxy) on which the change should be made. For newly introduced changes, preferably make changes only on the **experimental** channel. Note that **dev** channel isn't supposed to be enabled by users, so changes shouldn't really be made there
   3. Bump the "version" property in *module-config.yaml*
   4. Adjust main container image of the Operator Deployment manifest, located in the *connectivity-proxy.yaml* file. For example in the **experimental** channel it's [here](https://github.tools.sap/kyma/module-manifests/blob/main/modules/connectivity-proxy/experimental/connectivity-proxy.yaml)
   5. If needed, adjust the Connectivity Proxy CRD, which is located in the same file as the Operator Deployment  
   6. If needed, adjust the Connectivity Proxy default CR located in the *connectivity-proxy-default-cr.yaml* file. For example in the **experimental** channel it's [here](https://github.tools.sap/kyma/module-manifests/blob/main/modules/connectivity-proxy/experimental/connectivity-proxy-default-cr.yaml)
