package dev.snowdrop.kind;

public class KindVersion {

    public static String getKubernetesVersion(String version) {

        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Kubernetes version must be in format: major.minor.patch");
        }

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        //System.out.printf("Major: %d, Minor: %d, Patch: %d%n", major, minor, patch);
        return new KubernetesVersionDescriptor(major,minor,patch).getKubernetesVersion();
    }

    public static String defaultKubernetesVersion() {
      return new KubernetesVersionDescriptor(1,29,14).getKubernetesVersion();
    }
}
