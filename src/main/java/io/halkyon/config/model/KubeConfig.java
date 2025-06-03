package io.halkyon.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class KubeConfig {
    private String apiVersion;
    private String kind;
    private String currentContext;
    private List<Cluster> clusters;
    private List<Context> contexts;
    private List<User> users;
    private Map<String, Object> preferences;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(final String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(final String kind) {
        this.kind = kind;
    }

    @JsonProperty("current-context")
    public String getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(String currentContext) {
        this.currentContext = currentContext;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }

    public List<Context> getContexts() {
        return contexts;
    }

    public void setContexts(List<Context> contexts) {
        this.contexts = contexts;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    @Override
    public String toString() {
        return "KubeConfig{" +
            "apiVersion='" + apiVersion + '\'' +
            ", kind='" + kind + '\'' +
            ", currentContext='" + currentContext + '\'' +
            ", clusters=" + clusters +
            ", contexts=" + contexts +
            ", users=" + users +
            ", preferences=" + preferences +
            '}';
    }
}
