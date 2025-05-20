package dev.snowdrop.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.snowdrop.config.model.KubeConfig;

import java.io.IOException;

import static dev.snowdrop.config.Deserialization.YAML_MAPPER;

public final class KubeConfigUtils {

    public KubeConfigUtils() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    public static KubeConfig parseKubeConfig(final String kubeconfig) {
        try {
            return YAML_MAPPER.readValue(kubeconfig, KubeConfig.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serializeKubeConfig(final KubeConfig config) {
        try {
            return YAML_MAPPER.writeValueAsString(config);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to serialize kubeconfig", e);
        }
    }
}
