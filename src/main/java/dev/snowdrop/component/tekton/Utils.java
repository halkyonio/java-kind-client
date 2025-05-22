package dev.snowdrop.component.tekton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
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

    public static final String TEKTON_DASHBOARD_NAME = "tekton-dashboard";
    public static final String TEKTON_INGRESS_HOST_NAME = "tekton.localtest.me";
    private static final String TEKTON_CONTROLLER_NAMESPACE = "tekton-pipelines";

    public static InputStream fetchTektonResourcesFromURL(String version) {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = new URL("https://github.com/tektoncd/pipeline/releases/download/" + version + "/release.yaml").openStream();
        } catch (Exception e) {
            LOGGER.error("The resources cannot be fetched from the tekton repository URL !");
            LOGGER.error(e);
        }
        return resourceAsStream;
    }

    public static InputStream fetchTektonDashboardResourcesFromURL(String version) {
        InputStream resourceAsStream = null;
        try {
            if (version == "latest") {
                resourceAsStream = new URL("https://storage.googleapis.com/tekton-releases/dashboard/latest/release.yaml").openStream();
            } else {
                resourceAsStream = new URL(String.format("https://storage.googleapis.com/tekton-releases/dashboard/previous/%s/release.yaml", version)).openStream();
            }
        } catch (Exception e) {
            LOGGER.error("The resources cannot be fetched from the tekton dashboard repository URL !");
            LOGGER.error(e);
        }
        return resourceAsStream;
    }

    public static void installTekton(KubernetesClient client, String version) {
        // Install the Tekton resources using the YAML manifest file
        List<HasMetadata> items = client.load(fetchTektonResourcesFromURL(version)).items();
        LOGGER.info("Deploying the tekton resources ...");
        for (HasMetadata item : items) {
            var res = client.resource(item).create();
            Assertions.assertNotNull(res);
        }

        // Waiting till the Tekton pods are ready/running ...
        waitTillPodSelectedByLabelsIsReady(client, Map.of("app.kubernetes.io/name", "controller", "app.kubernetes.io/part-of", "tekton-pipelines"), TEKTON_CONTROLLER_NAMESPACE);
    }

    public static void installTektonDashboard(KubernetesClient client, String version) {
        List<HasMetadata> items = client.load(fetchTektonDashboardResourcesFromURL(version)).items();
        LOGGER.info("Deploying the tekton dashboard resources ...");
        for (HasMetadata item : items) {
            var res = client.resource(item).inNamespace(TEKTON_CONTROLLER_NAMESPACE);
            res.create();
            Assertions.assertNotNull(res);
        }

        // Waiting till the Tekton dashboard pod is ready/running ...
        waitTillPodSelectedByLabelsIsReady(client, Map.of("app.kubernetes.io/name", "dashboard", "app.kubernetes.io/part-of", "tekton-dashboard"), TEKTON_CONTROLLER_NAMESPACE);

        // Create the Tekton dashboard ingress route
        LOGGER.info("Creating the ingress route for the tekton dashboard ...");
        Ingress tektonIngressRoute = new IngressBuilder()
            // @formatter:off
            .withNewMetadata()
              .withName("tekton-ui")
              .withNamespace(TEKTON_CONTROLLER_NAMESPACE)
            .endMetadata()
            .withNewSpec()
              .addNewRule()
                .withHost(TEKTON_INGRESS_HOST_NAME)
                .withNewHttp()
                  .addNewPath()
                    .withPath("/")
                    .withPathType("Prefix") // This field is mandatory
                    .withNewBackend()
                      .withNewService()
                        .withName(TEKTON_DASHBOARD_NAME)
                        .withNewPort().withNumber(9097).endPort()
                      .endService()
                    .endBackend()
                  .endPath()
                .endHttp()
              .endRule()
            .endSpec()
            .build();
            // @formatter:on
        client.resource(tektonIngressRoute).create();
    }
}