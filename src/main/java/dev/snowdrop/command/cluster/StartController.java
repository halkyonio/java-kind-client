package dev.snowdrop.command.cluster;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static dev.snowdrop.internal.controller.PackageController.initPackageController;

@CommandLine.Command(name = "run", description = "Run the shared informer watching the resources")
public class StartController implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartController.class);
    private static final String KUBECONFIG_PATH = "/Users/cmoullia/code/ch007m/java-kind-client/kind1-kube.conf";
    private static final String CRD_PATH = "/META-INF/fabric8/packages.halkyonio.io-v1.yml";
    @Override
    public Integer call() throws Exception {
        try {
            KubernetesClient client = new KubernetesClientBuilder().withConfig(Files.newInputStream(Path.of(KUBECONFIG_PATH))).build();

            // Install the CRD
            InputStream is = StartController.class.getResourceAsStream("CRD_PATH");
            var res = client.resource(is).create();
            Assertions.assertNotNull(res);

            // Start the Package controller
            initPackageController(client);
            return 0;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 1;
        }
    }
}
