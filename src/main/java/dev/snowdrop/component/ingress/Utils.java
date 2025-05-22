package dev.snowdrop.component.ingress;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static dev.snowdrop.config.KubernetesClientUtils.waitTillPodSelectedByLabelsIsReady;

public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    public static InputStream fetchIngressResourcesFromURL(String version) {
        InputStream resourceAsStream = null;
        try {
            if (version == "latest") {
                resourceAsStream = new URL(
                    "https://raw.githubusercontent.com/kubernetes/ingress-nginx/refs/heads/main/deploy/static/provider/kind/deploy.yaml")
                    .openStream();
            } else {
                resourceAsStream = new URL(
                    String.format("https://raw.githubusercontent.com/kubernetes/ingress-nginx/refs/tags/controller-%s/deploy/static/provider/kind/deploy.yaml",version)).openStream();
            }
        } catch (Exception e) {
            LOGGER.error("The resources cannot be fetched from the ingress nginx repository URL !");
            LOGGER.error(e);
        }
        return resourceAsStream;
    }

    public static void installIngress(KubernetesClient client, String version) {
        List<HasMetadata> items = client.load(fetchIngressResourcesFromURL(version)).items();
        LOGGER.info("Deploying the ingress controller resources ...");
        for (HasMetadata item : items) {
            var res = client.resource(item).create();
            Assertions.assertNotNull(res);
        }
        waitTillPodSelectedByLabelsIsReady(client, Map.of(
                "app.kubernetes.io/name", "ingress-nginx",
                "app.kubernetes.io/component", "controller"),
            "ingress-nginx");
    }
}
