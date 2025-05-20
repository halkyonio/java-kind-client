package dev.snowdrop.test;

import dev.snowdrop.config.model.KubeConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.snowdrop.config.KubeConfigUtils.parseKubeConfig;

public class KubeConfigParsingTest {
    @Test
    public void test() throws IOException {
        String cfg = new String(KubeConfigParsingTest.class.getResourceAsStream("/kube.conf").readAllBytes());
        KubeConfig kubeconfig = parseKubeConfig(cfg);
        System.out.println(kubeconfig);
    }

}
