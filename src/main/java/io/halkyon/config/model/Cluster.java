package io.halkyon.config.model;

public class Cluster {
    private ClusterSpec cluster;
    private String name;

    public ClusterSpec getCluster() {
        return cluster;
    }

    public void setCluster(ClusterSpec cluster) {
        this.cluster = cluster;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
