package io.halkyon.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.halkyon.config.model.KubeConfig;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import static io.halkyon.config.Deserialization.YAML_MAPPER;

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

    public static InputStream loadCustomResource(String url) {
        try {
            URL packageCRDUrl = new URL(url);
            URLConnection urlConnection = packageCRDUrl.openConnection();
            return urlConnection.getInputStream();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitTillCustomResourceReady(KubernetesClient client, String customResourceName) {
        client.apiextensions().v1().customResourceDefinitions().withName(customResourceName)
            .waitUntilCondition(c ->
                c != null &&
                    c.getStatus() != null &&
                    c.getStatus().getAcceptedNames() != null, 60, TimeUnit.SECONDS);
    }
}
