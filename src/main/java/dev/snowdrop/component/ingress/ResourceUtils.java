package dev.snowdrop.component.ingress;

import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URL;

public class ResourceUtils {
    private static final Logger LOG = Logger.getLogger(ResourceUtils.class);

    public static InputStream fetchIngressResourcesFromURL(String version) {
        InputStream resourceAsStream = null;
        try {
            if (version == "latest") {
                resourceAsStream = new URL(
                    "https://raw.githubusercontent.com/kubernetes/ingress-nginx/refs/heads/main/deploy/static/provider/kind/deploy.yaml")
                    .openStream();
            } else {
                resourceAsStream = new URL(
                    "https://raw.githubusercontent.com/kubernetes/ingress-nginx/refs/tags/controller-" + version
                        + "/deploy/static/provider/kind/deploy.yaml")
                    .openStream();
            }
        } catch (Exception e) {
            LOG.error("The resources cannot be fetched from the ingress nginx repository URL !");
            LOG.error(e);
        }
        return resourceAsStream;
    }
}
