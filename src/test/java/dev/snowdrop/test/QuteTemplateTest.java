package dev.snowdrop.test;

import dev.snowdrop.config.model.qute.KubeAdmConfig;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@io.quarkus.test.junit.QuarkusTest
public class QuteTemplateTest {
    @Inject
    @Location("kubeadm.yaml")
    Template kubeadm;

    @Inject
    @Location("cni.yaml")
    Template cni;

    @Test
    public void testKubeAdmConfig() throws IOException {
        KubeAdmConfig cfg = new KubeAdmConfig();
        cfg.setNodeName("my-kind");
        cfg.setNodeIp("10.88.0.51");
        cfg.setBindPort("6443");
        cfg.setMinNodePort("30000");
        cfg.setMaxNodePort("32767");
        cfg.setKubernetesVersion("v1.32.2");
        cfg.setPodSubnet("10.244.0.0/16");
        cfg.setServiceSubnet("10.96.0.0/16");

        var result = kubeadm.data("cfg",cfg).render();
        assertTrue(result.contains("clusterName: my-kind"));
        assertTrue(result.contains("node-ip: 10.88.0.51"));
        assertTrue(result.contains("advertiseAddress: 10.88.0.51"));
        assertTrue(result.contains("controlPlaneEndpoint: 10.88.0.51:6443"));
        assertTrue(result.contains("service-node-port-range: 30000-32767"));
        assertTrue(result.contains("kubernetesVersion: v1.32.2"));
        assertTrue(result.contains("podSubnet: 10.244.0.0/16"));
        assertTrue(result.contains("serviceSubnet: 10.96.0.0/16"));
        assertTrue(result.contains("apiServerEndpoint: my-kind-control-plane:6443"));
        assertTrue(result.contains("provider-id: kind://podman/my-kind/my-kind-control-plane"));
    }

    @Test
    public void testCNI() throws IOException {
        KubeAdmConfig cfg = new KubeAdmConfig();
        cfg.setPodSubnet("10.244.0.0/16");
        var result = cni.data("cfg", cfg).render();
        assertTrue(result.contains("value: 10.244.0.0/16"));
    }
}
