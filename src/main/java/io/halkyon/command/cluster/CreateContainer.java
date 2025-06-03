package io.halkyon.command.cluster;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.*;
import com.github.dockerjava.api.model.*;
import io.halkyon.Container;
import io.halkyon.config.ClientConfig;
import io.halkyon.config.model.KubeConfig;
import io.halkyon.config.model.qute.KubeAdmConfig;
import io.halkyon.config.model.qute.StorageConfig;
import io.halkyon.container.ImageUtils;
import io.halkyon.kind.KindKubernetesConfiguration;
import io.fabric8.kubernetes.api.model.Taint;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.Transferable;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.api.model.AccessMode.ro;
import static io.halkyon.config.KubeConfigUtils.*;
import static io.halkyon.container.ContainerUtils.getFreePortOnHost;
import static io.halkyon.internal.resource.ingress.Utils.installIngress;
import static io.halkyon.internal.resource.platform.Utils.installPlatformController;
import static io.halkyon.kind.KindVersion.defaultKubernetesVersion;
import static io.halkyon.kind.KubernetesConfig.*;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

// Example Subcommand: Create
@CommandLine.Command(name = "create", description = "Create a new container")
public class CreateContainer extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateContainer.class);
    private String volumeName = "kindcontainer-" + UUID.randomUUID();
    private static final Map<String, String> TMP_FILESYSTEMS = new HashMap<String, String>() {{
        put("/run", "rw");
        put("/tmp", "rw");
    }};

    @Inject
    @Location("kubeadm.yaml")
    Template kubeadm;

    @Inject
    @Location("cni.yaml")
    Template cni;

    @Inject
    @Location("storage.yaml")
    Template storage;

    @CommandLine.Parameters(index = "0", description = "The image name to use for the container")
    String containerName;

    @CommandLine.Option(names = {"-k", "--kube-version"}, description = "The version of the kubernetes cluster to use")
    String kubeVersion;

    @CommandLine.Option(names = {"-p", "--port"}, description = "Bind a container's port(s) to the host", arity = "1..*")
    List<String> ports;  // e.g., "8080:80", "443:443/tcp"

    @CommandLine.Option(names = {"-v", "--volume"}, description = "Bind a volume to the container", arity = "1..*")
    List<String> volumes; // e.g., "/host:/container:ro", "named-volume:/container:rw"

    @CommandLine.Option(names = {"-e", "--env"}, description = "Set environment variables", arity = "1..*")
    List<String> environment; // e.g., "VAR1=value1", "VAR2=value2"

    @Inject
    ClientConfig cfg;

    @Override
    public Integer call() {
        // TODO: Improve this code to merge the user's arguments with the application.properties
        LOGGER.info("Cluster name: " + cfg.name());
        LOGGER.info("Labels: " + cfg.labels());
        cfg.binding().forEach(b -> {
            LOGGER.info("Binding between host => container: {}:{}", b.hostPort(), b.containerPort());
        });

        try {
            /*
               Use the default kubernetes version if no version has been specified as: 1.29.14, 1.30.10, 1.31.6, 1.32.2
               See versions of the kind project: https://github.com/kubernetes-sigs/kind/releases/tag/v0.27.0
             */

            KindKubernetesConfiguration kkc = new KindKubernetesConfiguration();

            if (kubeVersion == null) {
                kubeVersion = defaultKubernetesVersion();
            }

            // TODO: Add a method to validate of the kube version matches an existing kind image !
            kkc.setKubernetesVersion(kubeVersion);

            // Create the Kind Image Name
            var kindImageName = getKindImageName(kubeVersion);

            // Pull image
            ImageUtils.pullImage(dockerClient, kindImageName);

            // Build the command
            CreateContainerCmd ccc = dockerClient.createContainerCmd(kindImageName)
                .withHostName(containerName);

            if (containerName != null) {
                ccc.withName(containerName);
            } else {
                ccc.withName(NODE_NAME);
            }

            final Volume varVolume = new Volume("/var/lib/containerd");
            final Volume modVolume = new Volume("/lib/modules");
            final Volume devMapperVolume = new Volume("/dev/mapper");
            final List<Volume> volumes = new ArrayList<>();
            volumes.add(varVolume);
            volumes.add(modVolume);
            volumes.add(devMapperVolume);

            final List<Bind> binds = new ArrayList<Bind>();
            binds.add(new Bind(volumeName, varVolume, true));
            binds.add(new Bind("/lib/modules", modVolume, ro));
            binds.add(new Bind("/dev/mapper", devMapperVolume));

            ccc.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init")
                .withTty(true)
                .withVolumes(volumes)
                .getHostConfig().withBinds(binds);

            List<PortBinding> pbs = new ArrayList<PortBinding>();
            // Bind the Kube API port with a free Host port
            pbs.add(PortBinding.parse(String.format("%s:%s", getFreePortOnHost(), KUBE_API_PORT)));
            // Add additional binding ports
            if (cfg.binding() != null) {
                cfg.binding().stream()
                    .map(cfb -> {
                        PortBinding binding = PortBinding.parse(String.format("%s:%s", cfb.hostPort(), cfb.containerPort()));
                        LOGGER.info("Added binding: {}:{}", cfb.hostPort(), cfb.containerPort());
                        return binding;
                    })
                    .forEach(pbs::add);
            }
            ccc.getHostConfig().withPortBindings(pbs);

            ccc.getHostConfig().withPrivileged(true);
            ccc.getHostConfig().withTmpFs(TMP_FILESYSTEMS);

            // Labels
            ccc.withLabels(Map.of(
                "io.x-k8s.kind.cluster", containerName,
                "io.x-k8s.kind.role", "control-plane"
            ));

            if (environment != null && !environment.isEmpty()) {
                ccc.withEnv(environment.toArray(new String[0]));
            }
            ccc.withEnv(
                "KUBECONFIG=/etc/kubernetes/admin.conf",
                "container=" + cfg.providerId()
            );

            // Add kind network
            ccc.getHostConfig().withNetworkMode(KIND_NETWORK);

            CreateContainerResponse containerResponse;
            try {
                containerResponse = ccc.exec();
                String containerId = containerResponse.getId();
                LOGGER.info("Container created with ID: {}", containerId);

                // Inspect the Container
                containerInfo = dockerClient.inspectContainerCmd(containerId).exec();

                // Start the container and examine its status and log
                LOGGER.info("Starting the container: {}", containerId);
                dockerClient.startContainerCmd(containerId)
                    .exec();
                LOGGER.info("Container started: {}", containerId);

                // Wait for the container to be running and the message to appear
                waitForLogMessage(containerId, "multi-user.target", 60);

                // TODO Find a better way to wait and use CountDownLatch
                TimeUnit.SECONDS.sleep(5);

                // TODO: Add next steps to create the kubernetes cluster, install CNI and storage
                containerInfo = dockerClient.inspectContainerCmd(containerId).exec();

                // Generate the kubeAdmConfig
                KubeAdmConfig kubeAdmConfig = kkc.prepareTemplateParams(containerInfo);
                kubeAdmConfig.setProviderId(cfg.providerId());

                // Create the kubeAdmConfig file and run kubeadm init
                kubeadmInit(containerInfo, kubeAdmConfig);

                // TODO: Do we have to cp /etc/kubernetes/admin.conf ~/.kube/config OR use KUBECONFIG env var or kubectl --kubeconfig=''

                // Render from the template the CNI resources file and deploy it on the cluster
                installCNI(kubeAdmConfig);

                // Deploy the local storage resources on the cluster (based on rancher.io/local-path)
                StorageConfig storageConfig = new StorageConfig();
                storageConfig.setVolumeBindingMode("WaitForFirstConsumer"); // WaitForFirstConsumer, Immediate
                installStorage(storageConfig);

                // TODO: kindcontainer taint the node to remove: NoSchedule from the master or control-plane
                // but the label don't include it in our case => node-role.kubernetes.io/control-plane: ""

                // Add to the kubeConfig's user file the cluster definition to access the cluster
                String kubeconfig = new String(getFileFromContainer(containerInfo.getId(), "/etc/kubernetes/admin.conf"), StandardCharsets.UTF_8);
                LOGGER.debug("Kubeconfig file of the server: {}", kubeconfig);
                kubeconfig = replaceServerInKubeconfig(getClusterIpAndPort(containerInfo), kubeconfig);
                LOGGER.debug("Kubeconfig where IP and Port have been changed for the host: {}", kubeconfig);

                String pathToConfigFile = String.format("%s-%s", containerInfo.getName().replaceAll("/", ""), "kube.conf");
                LOGGER.info("Your kubernetes cluster config file is available at: {}", pathToConfigFile);
                try (PrintWriter out = new PrintWriter(pathToConfigFile)) {
                    out.println(kubeconfig);
                }

                // Configure Fabric8 kubernetes client
                KubernetesClient client = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeconfig)).build();

                // Untaint the node
                untaintNode(client);

                // Install the CRD able to handle the installation of the (core) packages
                var res = client.resource(loadCustomResource("https://raw.githubusercontent.com/halkyonio/java-package-operator/refs/heads/main/resources/crds/packages.halkyon.io-v1.yml")).create();
                Assertions.assertNotNull(res);

                client.resource(loadCustomResource("https://raw.githubusercontent.com/halkyonio/java-package-operator/refs/heads/main/resources/crds/platforms.halkyon.io-v1alpha1.yml")).create();
                Assertions.assertNotNull(res);

                /*
                CustomResourceDefinitionList crds = client.apiextensions().v1().customResourceDefinitions().list();
                crds.getItems().forEach(crd -> {
                    LOGGER.info("Custom resource installed: {}", crd.getMetadata().getName());
                });
                */
                waitTillCustomResourceReady(client, "packages.halkyon.io");
                waitTillCustomResourceReady(client, "platforms.halkyon.io");

                installPlatformController(client, "latest");

                return 0;
            } catch (DockerClientException e) {
                LOGGER.warn("Timeout to get the kind container response ...");
                return 0;
            } catch (NotModifiedException e) {
                LOGGER.warn("Container is already running {}.", containerInfo.getId());
                return 1;
            } catch (ConflictException e) {
                LOGGER.error("The container is already in use by a container !");
                return 1;
            } catch (NotFoundException e) {
                LOGGER.error("Container not found: " + containerInfo.getId());
                return 1;
            } catch (InternalServerErrorException e) {
                if (e.getMessage().startsWith("Status 500: {\"cause\":\"that name is already in use\"")) {
                    LOGGER.error("Container with the same name already exist: {}", containerName);
                } else {
                    var msg = e.getMessage();
                    LOGGER.error(e.getMessage());
                }
                return 1;
            }
        } catch (Exception e) {
            LOGGER.error("Error starting the container {}: {}", containerInfo.getId(), e.getMessage(), e);
            return 1;
        } finally {
            closeDockerClient();
        }
    }

    private void untaintNode(KubernetesClient client) {
        client.nodes().list().getItems().forEach(node -> {
            asList("master", "control-plane").forEach(role -> {
                final String key = format("node-role.kubernetes.io/%s", role);
                final String effect = "NoSchedule";
                final String removeTaint = String.format("%s:%s-", key, effect);
                if (hasTaint(node, key, null, effect)) {
                    try {
                        String[] cmd = {
                            "kubectl",
                            "--kubeconfig=/etc/kubernetes/admin.conf",
                            "taint",
                            "node",
                            node.getMetadata().getName(),
                            removeTaint
                        };
                        LOGGER.debug("Execute command: {}", Arrays.stream(cmd).toList());
                        execInContainer(cmd);
                    } catch (final IOException | InterruptedException e) {
                        throw new RuntimeException("Failed to untaint node", e);
                    }
                }
            });
        });
    }

    private boolean hasTaint(final io.fabric8.kubernetes.api.model.Node node, final String key, final String value, final String effect) {
        return Optional.ofNullable(node.getSpec().getTaints()).orElse(emptyList()).stream()
            .anyMatch(t -> isTaint(t, key, value, effect));
    }

    private static boolean isTaint(final Taint t, final String key, final String value, final String effect) {
        if (!Objects.equals(t.getKey(), key)) {
            return false;
        }
        if (!Objects.equals(t.getValue(), value)) {
            return false;
        }
        return Objects.equals(t.getEffect(), effect);
    }

    private void kubeadmInit(InspectContainerResponse containerInfo, KubeAdmConfig kubeAdmConfig) throws IOException, InterruptedException {
        String result;
        String kubeAdmConfigPath = format("%s/%s", CONTAINER_WORKDIR, "kube-admin.conf");

        // Render the template => KubeAdminConfig YAML and write it to the kind container
        try {
            LOGGER.info("Render the KubeAdminConfig template ...");
            result = kubeadm.data("cfg", kubeAdmConfig).render();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOGGER.debug("KubeAdmConfig generated: {}", result);

        LOGGER.info("Writing container file: {}", kubeAdmConfigPath);
        copyFileToContainer(containerInfo, Transferable.of(result.getBytes(UTF_8)), kubeAdmConfigPath);

        LOGGER.info("Execute command: {}", "kubeadm init ...");
        try {
            execInContainer(
                "kubeadm", "init",
                "--skip-phases=preflight",
                // specify our generated config file
                "--config=" + kubeAdmConfigPath,
                "--skip-token-print",
                // Use predetermined node name
                "--node-name=" + kubeAdmConfig.getNodeName(),
                // increase verbosity for debugging
                "--v=6");

            LOGGER.info("Copy the kube-admin.conf file to /kind/kubeadm.conf. This is needed to allow to restart the cluster / container");
            execInContainer("cp",kubeAdmConfigPath, "/kind/kubeadm.conf");

        } catch (final RuntimeException | IOException | InterruptedException e) {
            try {
                LOGGER.error("{}", execInContainer("journalctl").getStdout(), "JOURNAL: ");
            } catch (final IOException | InterruptedException ex) {
                LOGGER.error("Could not retrieve journal.", ex);
            }
            throw e;
        }
    }

    private void installCNI(KubeAdmConfig kubeAdmConfig) {
        String result;
        String CNI_RESOURCES_PATH = CONTAINER_WORKDIR + "manifests/cni.yaml";
        // Render the template => CNI YAML and write it to the kind container
        try {
            LOGGER.info("Render the CNI template ...");
            result = cni.data("cfg", kubeAdmConfig).render();
            LOGGER.debug("CNI generated: {}", result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Writing CNI generated resources file: {}", CNI_RESOURCES_PATH);
        copyFileToContainer(containerInfo, Transferable.of(result.getBytes(UTF_8)), CNI_RESOURCES_PATH);

        String[] cmd = {
            "kubectl",
            "--kubeconfig=/etc/kubernetes/admin.conf",
            "apply",
            "-f",
            CNI_RESOURCES_PATH
        };

        try {
            LOGGER.debug("Execute command: {}", Arrays.stream(cmd).toList());
            execInContainer(cmd);
        } catch (final RuntimeException | IOException | InterruptedException e) {
            LOGGER.error("Fail to execute the kubectl cmd", e);
        }
    }

    private void installStorage(StorageConfig storageconfig) {
        String result;
        String STORAGE_RESOURCES_PATH = CONTAINER_WORKDIR + "manifests/storage.yaml";
        // Render the template => Storage YAML and write it to the kind container
        try {
            LOGGER.info("Render the Storage template ...");
            result = storage.data("cfg", storageconfig).render();
            LOGGER.debug("Storage generated: {}", result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Writing Storage generated resources file: {}", STORAGE_RESOURCES_PATH);
        copyFileToContainer(containerInfo, Transferable.of(result.getBytes(UTF_8)), STORAGE_RESOURCES_PATH);

        String[] cmd = {
            "kubectl",
            "--kubeconfig=/etc/kubernetes/admin.conf",
            "apply",
            "-f",
            STORAGE_RESOURCES_PATH
        };

        try {
            LOGGER.info("Execute command: {}", Arrays.stream(cmd).toList());
            execInContainer(cmd);
        } catch (final RuntimeException | IOException | InterruptedException e) {
            LOGGER.error("Fail to execute the kubectl cmd", e);
        }
    }

    private void waitForLogMessage(String containerId, String expectedMessage, int timeoutSeconds)
        throws InterruptedException, IOException {

        ResultCallback<Frame> logCallback = null;
        CountDownLatch latch = new CountDownLatch(1);

        // Use a ResultCallback to asynchronously read the logs
        logCallback = new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
                String logEntry = new String(frame.getPayload());
                if (logEntry.contains(expectedMessage)) {
                    LOGGER.info("Found expected message in logs: {}", logEntry);
                    latch.countDown();
                }
            }
        };

        try (Closeable logStream = dockerClient.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .withTailAll()
            .exec(logCallback)) {

            boolean messageAppeared = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!messageAppeared) {
                throw new RuntimeException("Timeout: Expected log message not found within " + timeoutSeconds + " seconds.");
            }
        }
    }

    public void copyFileToContainer(InspectContainerResponse containerInfo, Transferable transferable, String containerPath) {
        if (containerInfo.getId() == null) {
            throw new IllegalStateException("copyFileToContainer can only be used with created / running container");
        }

        try (
            PipedOutputStream pipedOutputStream = new PipedOutputStream();
            PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
            TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(pipedOutputStream)
        ) {
            Thread thread = new Thread(() -> {
                try {
                    tarArchive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                    tarArchive.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

                    transferable.transferTo(tarArchive, containerPath);
                } finally {
                    IOUtils.closeQuietly(tarArchive);
                }
            });

            thread.start();

            dockerClient
                .copyArchiveToContainerCmd(containerInfo.getId())
                .withTarInputStream(pipedInputStream)
                .withRemotePath("/")
                .exec();

            thread.join();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getFileFromContainer(String id, String path) {
        CopyArchiveFromContainerCmd copycmd = dockerClient.copyArchiveFromContainerCmd(id, path);
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        try {
            InputStream tarStream = copycmd.exec();
            TarArchiveInputStream tarInput = new TarArchiveInputStream(tarStream);
            try {
                TarArchiveEntry tarEntry = tarInput.getNextTarEntry();
                while (tarEntry != null) {
                    copy(tarInput, file);
                    file.close();
                    tarEntry = tarInput.getNextTarEntry();
                }
                return file.toByteArray();
            } finally {
                if (tarInput != null) tarInput.close();
            }
        } catch (NotFoundException nfe) {
            throw new RuntimeException("Unable to locate container '" + id + "'", nfe);
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve '" + path + "' from container", e);
        }
    }

    private final void copy(InputStream in, OutputStream out) {
        byte[] buf = new byte[8192];
        int length;
        try {
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String replaceServerInKubeconfig(final String server, final String string) {
        final KubeConfig kubeconfig = parseKubeConfig(string);
        kubeconfig.getClusters().get(0).getCluster().setServer(server);
        return serializeKubeConfig(kubeconfig);
    }

    public static String getClusterIpAndPort(InspectContainerResponse containerInfo) {
        Map<ExposedPort, Ports.Binding[]> ports = containerInfo.getHostConfig().getPortBindings().getBindings();
        Ports.Binding[] bindings = ports.get(new ExposedPort(6443,InternetProtocol.TCP));
        return format("https://%s:%s", "127.0.0.1", bindings[0].getHostPortSpec());
    }

}
