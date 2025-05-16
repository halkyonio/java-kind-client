package dev.snowdrop.kind;

public class KubernetesConfig {

    public static final String CONTAINER_WORKDIR = "/kindcontainer";

    public static String KUBE_API_PORT = "6443";

    public static String getKindImageName(String kubeVersion) {
      return String.format("kindest/node:v%s", kubeVersion);
    }

    /*
      Name of the Cluster. Default to: kind

      apiVersion: kubeadm.k8s.io/v1beta3
      kind: ClusterConfiguration
      clusterName: <NODE_NAME>

    */
    public static String NODE_NAME = "KIND";

    /*

       Node IP which is the Container internal IP address

       It is used to configure to following fields:
       - advertiseAddress
       - node-ip
       of the "InitConfiguration" or "JoinConfiguration"

    */
    public static String NODE_IP = "10.88.0.51";

    /*
       Used to configure the Cluster Network

       apiVersion: kubeadm.k8s.io/v1beta3
       kind: ClusterConfiguration
       apiServer:
       networking:
         podSubnet: <POD_SUBNET>
         serviceSubnet: <SERVICE_SUBNET>

    */
    public static String POD_SUBNET = "10.244.0.0/16";
    public static String SERVICE_SUBNET = "10.245.0.0/16";

    /*
      Range of ports to be assigned to a Kubernetes Service and to avoid conflicts with reserved port numbers and other ports on a node.

      apiVersion: kubeadm.k8s.io/v1beta3
      kind: ClusterConfiguration
      apiServer:
        extraArgs:
          service-node-port-range: MIN_NODE_PORT-MAX_NODE_PORT
    */
    public static int MIN_NODE_PORT = 30000;
    public static int MAX_NODE_PORT = 32767;

    /*
     The controlPlaneEndpoint represents the Kube API endpoint.name followed by the internal Port number
     It can be defined using the node name followed by "-control-plane" and its port "6443" but also as IP

     apiVersion: kubeadm.k8s.io/v1beta3
     kind: ClusterConfiguration
     apiServer:
       extraArgs:
         service-node-port-range: MIN_NODE_PORT-MAX_NODE_PORT
    */
    public static String CONTROL_PLANE_ENDPOINT = NODE_NAME + "-control-plane";

    /*
       The KubernetesVersion of the Cluster which maps the kind image tag, version used

       apiVersion: kubeadm.k8s.io/v1beta3
       kind: ClusterConfiguration
       kubernetesVersion: v1.32.2

    */
    public static String KUBERNETES_VERSION = "v1.32.2";

    /*
        The provider-ip of the Init or Join Configuration

        kind: InitConfiguration
        localAPIEndpoint:
          advertiseAddress: <NODE_IP>
          bindPort: 6443
        nodeRegistration:
          criSocket: unix:///run/containerd/containerd.sock
          kubeletExtraArgs:
            provider-id: <PROVIDER_ID> # podman or docker

    */
    public static String PROVIDER_ID = "kind://podman/my-kind/my-kind-control-plane";
}
