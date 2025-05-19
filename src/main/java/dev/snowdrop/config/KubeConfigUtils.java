package dev.snowdrop.config;

import dev.snowdrop.config.model.KubeConfig;

import java.io.IOException;

import static dev.snowdrop.config.Deserialization.JSON_MAPPER;

public final class KubeConfigUtils {

    public KubeConfigUtils() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    public static KubeConfig parseKubeConfig(final String kubeconfig) {
        try {
            // Add method to load the KUBE config file from PATH = kubeconfig
            return JSON_MAPPER.readValue("", KubeConfig.class);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to deserialize kubeconfig", e);
        }
    }

    public static String serializeKubeConfig(final KubeConfig config) {
        try {
            // Add method to load the KUBE config file from PATH = kubeconfig
            return JSON_MAPPER.writeValueAsString(config);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to serialize kubeconfig", e);
        }
    }

    public static String replaceServerInKubeconfig(final String server, final String string) {
        final KubeConfig kubeconfig = parseKubeConfig(string);
        kubeconfig.getClusters().get(0).getCluster().setServer(server);
        return serializeKubeConfig(kubeconfig);
    }
}
