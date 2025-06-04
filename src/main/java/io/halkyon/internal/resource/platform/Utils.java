package io.halkyon.internal.resource.platform;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static io.halkyon.config.KubeConfigUtils.loadCustomResource;
import static io.halkyon.config.KubernetesClientUtils.waitTillPodSelectedByLabelsIsReady;

public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    private static final String PLATFORM_CONTROLLER_NAMESPACE = "platform";

    public static InputStream fetchPlatformResourcesFromURL(String version) {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = new URL("https://raw.githubusercontent.com/halkyonio/java-package-operator/refs/heads/main/resources/manifests/kubernetes.yml").openStream();
        } catch (Exception e) {
            LOGGER.error("The resources cannot be fetched from the tekton repository URL !");
            LOGGER.error(e);
        }
        return resourceAsStream;
    }

    public static void installPlatformController(KubernetesClient client, String version) {
        List<HasMetadata> items;
        HasMetadata res;

        // TODO: The RBAC should be merged with the controller resources
        // Create the namespace and RBAC
        try {
            items = client.load(new URL("https://raw.githubusercontent.com/halkyonio/java-package-operator/refs/heads/main/resources/manifests/rbac-cluster-admin-sa-default.yml").openStream()).items();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Deploying the Platform resources: rbac, namespace, etc ...");
        for (HasMetadata item : items) {
            res = client.resource(item).create();
            Assertions.assertNotNull(res);
        }

        // Install the CRD
        res = client.resource(loadCustomResource("https://raw.githubusercontent.com/halkyonio/java-package-operator/refs/heads/main/resources/crds/packages.halkyon.io-v1.yml")).create();
        Assertions.assertNotNull(res);

        client.resource(loadCustomResource("https://raw.githubusercontent.com/halkyonio/java-package-operator/refs/heads/main/resources/crds/platforms.halkyon.io-v1.yml")).create();
        Assertions.assertNotNull(res);

        items = client.load(fetchPlatformResourcesFromURL(version)).items();
        LOGGER.info("Deploying the Platform controller resources ...");
        for (HasMetadata item : items) {
            res = client.resource(item).inNamespace(PLATFORM_CONTROLLER_NAMESPACE).create();
            Assertions.assertNotNull(res);
        }

        // Waiting till the controller pod is ready/running ...
        waitTillPodSelectedByLabelsIsReady(client, Map.of("app.kubernetes.io/name", "package-operator", "app.kubernetes.io/version","0.1.0-SNAPSHOT"), PLATFORM_CONTROLLER_NAMESPACE);
    }
}