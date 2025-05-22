package dev.snowdrop.internal.crd;

import dev.snowdrop.internal.model.KubernetesJob;

import java.util.Map;

public class PackageSpec {
    private String tool;
    private String url;
    private String version;
    private Map<String, Object> values;
    private KubernetesJob kubernetesJob;

    // Getters and Setters
    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public KubernetesJob getKubernetesJob() {
        return kubernetesJob;
    }

    public void setKubernetesJob(KubernetesJob kubernetesJob) {
        this.kubernetesJob = kubernetesJob;
    }

    @Override
    public String toString() {
        return "PackageSpec{" +
            "tool='" + tool + '\'' +
            ", url='" + url + '\'' +
            ", version='" + version + '\'' +
            ", values=" + values +
            ", kubernetesJob=" + kubernetesJob +
            '}';
    }
}
