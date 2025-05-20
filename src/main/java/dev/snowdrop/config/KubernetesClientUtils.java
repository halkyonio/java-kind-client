package dev.snowdrop.config;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KubernetesClientUtils {
    private static final long TIME_OUT = 360;
    private static final Logger LOG = Logger.getLogger(KubernetesClientUtils.class);

    public static void waitTillPodSelectedByLabelsIsReady(KubernetesClient client, Map<String, String> labels, String ns) {
        client.resources(Pod.class)
            .inNamespace(ns)
            .withLabels(labels)
            .waitUntilReady(TIME_OUT, TimeUnit.SECONDS);
        LOG.infof("Pod selected with labels: %s is ready", labels);
    }
}
