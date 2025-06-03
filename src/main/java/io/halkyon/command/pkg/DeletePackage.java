package io.halkyon.command.pkg;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.halkyon.platform.operator.crd.Package;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static io.halkyon.internal.controller.PackageSharedInformer.runInformer;

@CommandLine.Command(name = "delete", description = "Delete a package")
public class DeletePackage implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePackage.class);
    private final CountDownLatch latch = new CountDownLatch(1);

    @CommandLine.Parameters(index = "0", description = "The name of package to be deleted")
    String packageName;

    @CommandLine.ParentCommand
    PackageCommand parent;

    @Override
    public Integer call() throws Exception {
        System.setProperty("kubeconfig", parent.kubeConfigPath);
        KubernetesClient client = new KubernetesClientBuilder().build();
        boolean packageFound = false;

        try {
            for (Package pkg : client.resources(Package.class).list().getItems()) {
                if (pkg.getMetadata().getName().equals(packageName)) {
                    var result = client.resource(pkg).inNamespace(parent.namespace).delete();
                    Assertions.assertNotNull(result);
                    LOGGER.info("Package '{}' deleted successfully.", packageName);
                    packageFound = true;
                } else {
                    LOGGER.warn("Failed to delete package '{}'. It might not exist or there was a permission issue.", packageName);
                }
                // Assuming you only want to process one package matching the name
                break;
            }

            if (!packageFound) {
                LOGGER.warn("Package '{}' not found in namespace '{}'. No deletion performed.", packageName, parent.namespace);
                // You might choose to exit here if finding the package is a strict requirement
                // If you still want to run the controller even if deletion didn't happen, continue.
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutdown signal received. Cleaning up...");
                latch.countDown();
            }, "app-shutdown-hook"));

            LOGGER.info("Launching the Package informer to delete packages...");
            runInformer(client);

            try {
                latch.await(); // Wait for SIGTERM
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Main thread interrupted while waiting for shutdown signal.");
            }

            LOGGER.info("Application stopped.");
            return 0;

        } catch (KubernetesClientException kce) {
            LOGGER.error("Kubernetes client error. Message: {}, Exception: {}", kce.getMessage(), kce);
            return 1;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 1;
        } finally {
            // Ensure the KubernetesClient is closed when the application finishes
            if (client != null) {
                client.close();
                LOGGER.info("Kubernetes client closed.");
            }
        }
    }
}
