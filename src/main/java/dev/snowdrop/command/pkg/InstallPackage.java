package dev.snowdrop.command.pkg;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.Callable;

import static dev.snowdrop.internal.controller.PackageController.runPackageController;

@CommandLine.Command(name = "install", description = "Install a package")
public class InstallPackage implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstallPackage.class);

    @CommandLine.Option(
        names = {"-p","--package"},
        description = "The url of the package to be installed"
    )
    String packageResource;

    @CommandLine.ParentCommand
    PackageCommand parent;

    @Override
    public Integer call() throws Exception {
        System.setProperty("kubeconfig", parent.kubeConfigPath);
        KubernetesClient client = new KubernetesClientBuilder().build();

        try {
            if (packageResource != null) {
                InputStream is = new URL(validatePackageURL(packageResource)).openConnection().getInputStream();
                if (is == null) {
                    LOGGER.error("The package resource could not be found");
                    return 1;
                } else {
                    client.resource(is).inNamespace(parent.namespace).create();
                }
            }
            LOGGER.info("Launching the Package informer ...");
            runPackageController(client);
            Thread.sleep(30000);
            return 0;
        } catch (KubernetesClientException kce) {
            LOGGER.error("Kubernetes client error. Message: {}, Exception: {}", kce.getMessage(), kce);
            return 1;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 1;
        }
    }

    private static String validatePackageURL(String stringToParse) {
        boolean hasKnownPrefix = Set.of("http://", "https://", "file://").stream().anyMatch(s -> s.startsWith(stringToParse));
        if (hasKnownPrefix) {
            return stringToParse.trim().toLowerCase();
        } else {
            // We assume that the resource is a file
            return String.format("file://%s", stringToParse);
        }
    }
}
