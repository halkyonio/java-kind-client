package dev.snowdrop.component.tekton;

import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URL;

public class ResourceUtils {
    private static final Logger LOGGER = Logger.getLogger(ResourceUtils.class);

    public static final String TEKTON_DASHBOARD_NAME = "tekton-dashboard";
    public static final String TEKTON_INGRESS_HOST_NAME = "tekton.localtest.me";

    public static InputStream fetchTektonResourcesFromURL(String version) {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = new URL(
                "https://github.com/tektoncd/pipeline/releases/download/" + version + "/release.yaml")
                .openStream();
        } catch (Exception e) {
            LOGGER.error("The resources cannot be fetched from the tekton repository URL !");
            LOGGER.error(e);
        }
        return resourceAsStream;
    }

    public static InputStream fetchTektonDashboardResourcesFromURL() {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = new URL(
                "https://storage.googleapis.com/tekton-releases/dashboard/latest/release.yaml").openStream();
        } catch (Exception e) {
            LOGGER.error("The resources cannot be fetched from the tekton dashboard repository URL !");
            LOGGER.error(e);
        }
        return resourceAsStream;
    }
}