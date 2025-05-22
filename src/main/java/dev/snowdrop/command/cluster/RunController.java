package dev.snowdrop.command.cluster;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static dev.snowdrop.internal.controller.PackageController.initPackageController;
import static java.lang.String.format;

@CommandLine.Command(name = "run-controller", description = "Run the shared informer watching the resources")
public class RunController implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunController.class);
    private static final String KUBECONFIG_PATH = "/Users/cmoullia/code/ch007m/java-kind-client/kind1-kube.conf";
    private static final String CRD_PACKAGE_FILE_NAME = "packages.halkyon.io-v1.yml";
    private static final String CRD_PATH = "crds/" + CRD_PACKAGE_FILE_NAME;

    @Override
    public Integer call() throws Exception {
        System.setProperty("KUBECONFIG", KUBECONFIG_PATH);
        KubernetesClient client = new DefaultKubernetesClient();

        final String EXECUTION_DIR = System.getProperty("user.dir");
        LOGGER.info("current dir = " + EXECUTION_DIR);

        // Install the CRD
        try (InputStream is = new FileInputStream(format("%s/%s",EXECUTION_DIR,CRD_PATH))) {
            var res = client.resource(is).create();
            Assertions.assertNotNull(res);

            CustomResourceDefinitionList crds = client.apiextensions().v1().customResourceDefinitions().list();
            crds.getItems().forEach(crd -> {
                LOGGER.info("Name: {}", crd.getMetadata().getName());
            });

            LOGGER.info("Waiting for CRD to be established...");
            client.apiextensions().v1().customResourceDefinitions().withName("packages.halkyon.io")
                .waitUntilCondition(c ->
                    c != null &&
                        c.getStatus() != null &&
                        c.getStatus().getAcceptedNames() != null, 60, TimeUnit.SECONDS);
            LOGGER.info("CRD deployed successfully: {}", CRD_PATH);

            client.resource(RunController.class.getClassLoader().getResourceAsStream("packages/ingress.yml")).inNamespace("default").create();
            LOGGER.info("Package YAML deployed successfully.");

            // Start the Package controller
            LOGGER.info("Launching the Package informer ...");
            initPackageController(client);

            return 0;

        } catch (IOException ioe) {
            LOGGER.error(ioe.getMessage());
            return 1;
        } catch (KubernetesClientException kce) {
            LOGGER.error("Kubernetes client error. Message: {}, Exception: {}", kce.getMessage(), kce);
            return 1;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return 1;
        }
    }
}
