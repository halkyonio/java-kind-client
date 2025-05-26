package dev.snowdrop.command.pkg;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.halkyon.pkg.crd.Package;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static dev.snowdrop.internal.controller.PackageController.runPackageController;

@CommandLine.Command(name = "delete", description = "Delete a package")
public class DeletePackage implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePackage.class);

    @CommandLine.Parameters(index = "0", description = "The name of package to be deleted")
    String packageName;

    @CommandLine.ParentCommand
    PackageCommand parent;

    @Override
    public Integer call() throws Exception {
        System.setProperty("kubeconfig", parent.kubeConfigPath);
        KubernetesClient client = new KubernetesClientBuilder().build();

        try {
            client.resources(Package.class).list().getItems().forEach(pkg -> {
                if (pkg.getMetadata().getName().equals(packageName)) {
                    var result = client.resource(pkg).inNamespace(parent.namespace).delete();
                    Assertions.assertNotNull(result);

                    // Start the Package controller
                    LOGGER.info("Launching the Package informer to delete packages...");
                    runPackageController(client);

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return 0;

        } catch (KubernetesClientException kce) {
            LOGGER.error("Kubernetes client error. Message: {}, Exception: {}", kce.getMessage(), kce);
            return 1;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 1;
        }
    }
}
