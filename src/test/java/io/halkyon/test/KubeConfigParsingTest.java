package io.halkyon.test;

import io.halkyon.config.model.KubeConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.halkyon.config.KubeConfigUtils.parseKubeConfig;

public class KubeConfigParsingTest {
    @Test
    public void test() throws IOException {
        String cfg = new String(KubeConfigParsingTest.class.getResourceAsStream("/kube.conf").readAllBytes());
        KubeConfig kubeconfig = parseKubeConfig(cfg);
        System.out.println(kubeconfig);
    }

}
