package io.halkyon.command.cluster;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.*;
import com.github.dockerjava.api.model.*;
import io.fabric8.kubernetes.api.model.Taint;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.halkyon.Container;
import io.halkyon.config.ClientConfig;
import io.halkyon.config.model.KubeConfig;
import io.halkyon.config.model.qute.KubeAdmConfig;
import io.halkyon.config.model.qute.StorageConfig;
import io.halkyon.container.ImageUtils;
import io.halkyon.kind.KindKubernetesConfiguration;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlLocationConfigSourceFactory;
import jakarta.inject.Inject;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.Transferable;
import picocli.CommandLine;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.api.model.AccessMode.ro;
import static io.halkyon.config.KubeConfigUtils.*;
import static io.halkyon.container.ContainerUtils.getFreePortOnHost;
import static io.halkyon.internal.resource.platform.Utils.installPlatformController;
import static io.halkyon.kind.KindVersion.defaultKubernetesVersion;
import static io.halkyon.kind.KubernetesConfig.*;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@CommandLine.Command(name = "create", description = "Create a new container")
public class CreateContainer extends Container implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(CreateContainer.class);
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

    @CommandLine.Option(names = {"-c", "--config-file"}, description = "The YAML config file")
    File Configfile;

    @CommandLine.Option(names = {"-p", "--port"}, description = "Bind a container's port(s) to the host", arity = "1..*")
    List<String> ports;  // e.g., "8080:80", "443:443/tcp"

    @CommandLine.Option(names = {"-v", "--volume"}, description = "Bind a volume to the container", arity = "1..*")
    List<String> volumes; // e.g., "/host:/container:ro", "named-volume:/container:rw"

    @CommandLine.Option(names = {"-e", "--env"}, description = "Set environment variables", arity = "1..*")
    List<String> environment; // e.g., "VAR1=value1", "VAR2=value2"

    ClientConfig cfg;

    @Override
    public Integer call() {

        try {

            SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
            SmallRyeConfigBuilder configBuilder = new SmallRyeConfigBuilder();
            SmallRyeConfig smallRyeConfig;

            if (Configfile != null) {
                if (!Configfile.exists() || !Configfile.isFile()) {
                    // TODO to be reviewed to handle the 2 use cases: not found or not provided
                    LOG.warn("User's config file not provided or not found: {}", Configfile.getAbsolutePath());
                }
                LOG.info("External config file: {}", Configfile.getAbsolutePath());

                System.setProperty(SMALLRYE_CONFIG_LOCATIONS, Configfile.getAbsolutePath());
                smallRyeConfig = configBuilder
                    .withMapping(ClientConfig.class)
                    .withValidateUnknown(false)
                    .withSources(new YamlLocationConfigSourceFactory() {
                        @Override
                        protected ConfigSource loadConfigSource(URL url, int ordinal) throws IOException {
                            return super.loadConfigSource(url, 500);
                        }
                    })
                    .withSources(new ConfigSource() {
                        @Override
                        public Set<String> getPropertyNames() {
                            Set<String> properties = new HashSet<>();
                            config.getPropertyNames().forEach(properties::add);
                            return properties;
                        }

                        @Override
                        public String getValue(final String propertyName) {
                            return config.getRawValue(propertyName);
                        }

                        @Override
                        public String getName() {
                            return "Client Config";
                        }
                    })
                    .build();
                cfg = smallRyeConfig.getConfigMapping(ClientConfig.class);
            } else {
                smallRyeConfig = configBuilder
                    .withMapping(ClientConfig.class)
                    .withValidateUnknown(false)
                    .withSources(new ConfigSource() {
                        @Override
                        public Set<String> getPropertyNames() {
                            Set<String> properties = new HashSet<>();
                            config.getPropertyNames().forEach(properties::add);
                            return properties;
                        }

                        @Override
                        public String getValue(final String propertyName) {
                            return config.getRawValue(propertyName);
                        }

                        @Override
                        public String getName() {
                            return "Client Config";
                        }
                    })
                    .build();
                cfg = smallRyeConfig.getConfigMapping(ClientConfig.class);
            }
            LOG.info("Kube version: " + cfg.kubernetesVersion().get());
            LOG.info("Name: " + cfg.name());
            LOG.info("Provider: " + cfg.providerId());
            LOG.info("Labels: " + cfg.labels().get());

            // Check if the user provided a kubernetes version (command line or YAML file), otherwise use the default
            setKubernetesVersion();

            KindKubernetesConfiguration kkc = new KindKubernetesConfiguration();

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
                        LOG.info("Added binding: {}:{}", cfb.hostPort(), cfb.containerPort());
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
                LOG.info("Container created with ID: {}", containerId);

                // Inspect the Container
                containerInfo = dockerClient.inspectContainerCmd(containerId).exec();

                // Start the container and examine its status and log
                LOG.info("Starting the container: {}", containerId);
                dockerClient.startContainerCmd(containerId)
                    .exec();
                LOG.info("Container started: {}", containerId);

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
                LOG.info("Preparing nodes \uD83D\uDCE6");
                kubeadmInit(containerInfo, kubeAdmConfig);
                LOG.info("Starting control-plane \uD83D\uDD79\uFE0F");

                // TODO: Do we have to cp /etc/kubernetes/admin.conf ~/.kube/config OR use KUBECONFIG env var or kubectl --kubeconfig=''

                // Render from the template the CNI resources file and deploy it on the cluster
                LOG.info("Installing CNI  \uD83D\uDD0C");
                installCNI(kubeAdmConfig);

                LOG.info("Installing StorageClass  \uD83D\uDCBE");
                // Deploy the local storage resources on the cluster (based on rancher.io/local-path)
                StorageConfig storageConfig = new StorageConfig();
                storageConfig.setVolumeBindingMode("WaitForFirstConsumer"); // WaitForFirstConsumer, Immediate
                installStorage(storageConfig);

                // TODO: kindcontainer taint the node to remove: NoSchedule from the master or control-plane
                // but the label don't include it in our case => node-role.kubernetes.io/control-plane: ""

                // Add to the kubeConfig's user file the cluster definition to access the cluster
                String kubeconfig = new String(getFileFromContainer(containerInfo.getId(), "/etc/kubernetes/admin.conf"), StandardCharsets.UTF_8);
                LOG.debug("Kubeconfig file of the server: {}", kubeconfig);
                kubeconfig = replaceServerInKubeconfig(getClusterIpAndPort(containerInfo), kubeconfig);
                LOG.debug("Kubeconfig where IP and Port have been changed for the host: {}", kubeconfig);

                String pathToConfigFile = String.format("%s-%s", containerInfo.getName().replaceAll("/", ""), "kube.conf");
                try (PrintWriter out = new PrintWriter(pathToConfigFile)) {
                    out.println(kubeconfig);
                }

                // Configure Fabric8 kubernetes client
                KubernetesClient client = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeconfig)).build();

                // Untaint the node
                untaintNode(client);

                // Wait till the pods of the cluster are ready/running
                waitTillPodRunning(client, "kube-system", Map.of("component", "kube-apiserver"));
                waitTillPodRunning(client, "kube-system", Map.of("component", "etcd"));
                waitTillPodRunning(client, "kube-system", Map.of("k8s-app", "kube-dns"));
                waitTillPodRunning(client, "local-path-storage", Map.of("app", "local-path-provisioner"));

                installPlatformController(client, "latest");

                LOG.info("Your Quarkus Kind cluster is ready ! \uD83D\uDC4B\n");
                LOG.info("You can now use your cluster with:\n");
                LOG.info("kubectl --kubeconfig={}", pathToConfigFile);

                return 0;
            } catch (DockerClientException e) {
                LOG.warn("Timeout to get the kind container response ...");
                return 1;
            } catch (NotModifiedException e) {
                LOG.warn("Container is already running {}.", containerInfo.getId());
                return 1;
            } catch (ConflictException e) {
                LOG.error("The container is already in use by a container !");
                return 1;
            } catch (NotFoundException e) {
                LOG.error("Container not found: " + containerInfo.getId());
                return 1;
            } catch (InternalServerErrorException e) {
                if (e.getMessage().startsWith("Status 500: {\"cause\":\"that name is already in use\"")) {
                    LOG.error("Container with the same name already exist: {}", containerName);
                } else {
                    LOG.error(e.getMessage());
                }
                return 1;
            }
        } catch (Exception e) {
            LOG.error("Error creating the container using the engine: {}.\n{}", cfg.providerId(), e.getMessage());
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
                        LOG.debug("Execute command: {}", Arrays.stream(cmd).toList());
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
            LOG.info("Render the KubeAdminConfig template ...");
            result = kubeadm.data("cfg", kubeAdmConfig).render();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOG.debug("KubeAdmConfig generated: {}", result);

        LOG.info("Writing container file: {}", kubeAdmConfigPath);
        copyFileToContainer(containerInfo, Transferable.of(result.getBytes(UTF_8)), kubeAdmConfigPath);

        LOG.info("Execute command: {}", "kubeadm init ...");
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

            LOG.info("Copy the kube-admin.conf file to /kind/kubeadm.conf. This is needed to allow to restart the cluster / container");
            execInContainer("cp", kubeAdmConfigPath, "/kind/kubeadm.conf");

        } catch (final RuntimeException | IOException | InterruptedException e) {
            try {
                LOG.error("{}", execInContainer("journalctl").getStdout(), "JOURNAL: ");
            } catch (final IOException | InterruptedException ex) {
                LOG.error("Could not retrieve journal.", ex);
            }
            throw e;
        }
    }

    private void installCNI(KubeAdmConfig kubeAdmConfig) {
        String result;
        String CNI_RESOURCES_PATH = CONTAINER_WORKDIR + "manifests/cni.yaml";
        // Render the template => CNI YAML and write it to the kind container
        try {
            LOG.info("Render the CNI template ...");
            result = cni.data("cfg", kubeAdmConfig).render();
            LOG.debug("CNI generated: {}", result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOG.info("Writing CNI generated resources file: {}", CNI_RESOURCES_PATH);
        copyFileToContainer(containerInfo, Transferable.of(result.getBytes(UTF_8)), CNI_RESOURCES_PATH);

        String[] cmd = {
            "kubectl",
            "--kubeconfig=/etc/kubernetes/admin.conf",
            "apply",
            "-f",
            CNI_RESOURCES_PATH
        };

        try {
            LOG.debug("Execute command: {}", Arrays.stream(cmd).toList());
            execInContainer(cmd);
        } catch (final RuntimeException | IOException | InterruptedException e) {
            LOG.error("Fail to execute the kubectl cmd", e);
        }
    }

    private void installStorage(StorageConfig storageconfig) {
        String result;
        String STORAGE_RESOURCES_PATH = CONTAINER_WORKDIR + "manifests/storage.yaml";
        // Render the template => Storage YAML and write it to the kind container
        try {
            LOG.info("Render the Storage template ...");
            result = storage.data("cfg", storageconfig).render();
            LOG.debug("Storage generated: {}", result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOG.info("Writing Storage generated resources file: {}", STORAGE_RESOURCES_PATH);
        copyFileToContainer(containerInfo, Transferable.of(result.getBytes(UTF_8)), STORAGE_RESOURCES_PATH);

        String[] cmd = {
            "kubectl",
            "--kubeconfig=/etc/kubernetes/admin.conf",
            "apply",
            "-f",
            STORAGE_RESOURCES_PATH
        };

        try {
            LOG.info("Execute command: {}", Arrays.stream(cmd).toList());
            execInContainer(cmd);
        } catch (final RuntimeException | IOException | InterruptedException e) {
            LOG.error("Fail to execute the kubectl cmd", e);
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
                    LOG.info("Found expected message in logs: {}", logEntry);
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
        Ports.Binding[] bindings = ports.get(new ExposedPort(6443, InternetProtocol.TCP));
        return format("https://%s:%s", "127.0.0.1", bindings[0].getHostPortSpec());
    }

    public void setKubernetesVersion() {
        this.kubeVersion = Optional.ofNullable(this.kubeVersion)
            .or(() -> cfg.kubernetesVersion())
            .orElse(defaultKubernetesVersion());
    }
}
