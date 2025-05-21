package dev.snowdrop.config.model.qute;

public class KubeAdmConfig {
    private String nodeName;
    private String nodeIp;
    private String bindPort;
    private String minNodePort;
    private String maxNodePort;
    private String podSubnet;
    private String serviceSubnet;
    private String kubernetesVersion;
    private String nodeLabels;
    private String providerId;

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getBindPort() {
        return bindPort;
    }

    public void setBindPort(String bindPort) {
        this.bindPort = bindPort;
    }

    public String getPodSubnet() {
        return podSubnet;
    }

    public void setPodSubnet(String podSubnet) {
        this.podSubnet = podSubnet;
    }

    public String getServiceSubnet() {
        return serviceSubnet;
    }

    public void setServiceSubnet(String serviceSubnet) {
        this.serviceSubnet = serviceSubnet;
    }

    public String getMinNodePort() {
        return minNodePort;
    }

    public void setMinNodePort(String minNodePort) {
        this.minNodePort = minNodePort;
    }

    public String getMaxNodePort() {
        return maxNodePort;
    }

    public void setMaxNodePort(String maxNodePort) {
        this.maxNodePort = maxNodePort;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getKubernetesVersion() {
        return kubernetesVersion;
    }

    public void setKubernetesVersion(String kubernetesVersion) {
        this.kubernetesVersion = kubernetesVersion;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    public String getNodeLabels() {
        return nodeLabels;
    }

    public void setNodeLabels(String nodeLabels) {
        this.nodeLabels = nodeLabels;
    }
}
