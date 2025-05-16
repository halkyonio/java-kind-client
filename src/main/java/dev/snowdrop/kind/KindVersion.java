package dev.snowdrop.kind;

public class KindVersion {

    public static String defaultKubernetesVersion() {
      return new KubernetesVersionDescriptor(1,29,14).getKubernetesVersion();
    }
}
