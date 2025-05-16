package dev.snowdrop.command;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import dev.snowdrop.Container;
import dev.snowdrop.container.ImageUtils;
import dev.snowdrop.kind.KindKubernetesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.api.model.AccessMode.ro;
import static dev.snowdrop.kind.KindVersion.defaultKubernetesVersion;
import static dev.snowdrop.kind.KubernetesConfig.KUBE_API_PORT;
import static dev.snowdrop.kind.KubernetesConfig.getKindImageName;
import static dev.snowdrop.kind.PortUtils.getFreePortOnHost;

// Example Subcommand: Create
@CommandLine.Command(name = "create", description = "Create a new container")
public class CreateContainer extends Container implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateContainer.class);
    private String volumeName = "kindcontainer-" + UUID.randomUUID();
    private static final Map<String, String> TMP_FILESYSTEMS = new HashMap<String, String>() {{
        put("/run", "rw");
        put("/tmp", "rw");
    }};

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

    @Override
    public Integer call() {
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
            CreateContainerCmd ccc = dockerClient.createContainerCmd(kindImageName);

            if (containerName != null) {
                ccc.withName(containerName);
            }

            ccc.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init");
            ccc.getHostConfig().withPortBindings(PortBinding.parse(String.format("%s:%s", getFreePortOnHost(), KUBE_API_PORT)));

            final Volume varVolume = new Volume("/var/lib/containerd");
            final Volume modVolume = new Volume("/lib/modules");
            final List<Volume> volumes = new ArrayList<>();
            volumes.add(varVolume);
            volumes.add(modVolume);
            ccc.withVolumes(volumes);

            final List<Bind> binds = new ArrayList<>();
            binds.add(new Bind(volumeName, varVolume, true));
            binds.add(new Bind("/lib/modules", modVolume, ro));
            ccc.withBinds(binds);

            ccc.getHostConfig().withPrivileged(true);
            ccc.getHostConfig().withTmpFs(TMP_FILESYSTEMS);

            if (environment != null && !environment.isEmpty()) {
                ccc.withEnv(environment.toArray(new String[0]));
            }
            ccc.withEnv("KUBECONFIG", "/etc/kubernetes/admin.conf");

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
                waitForLogMessage(containerId, "starting init", 60, TimeUnit.SECONDS);

/*                LOGGER.info("- attaching log relay");
                // grab the logs to stdout.
                LogConfig lc = new LogConfig("info", true, null);
                dockerClient.logContainerCmd(containerId)
                    .withFollowStream(true)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTimestamps(true)
                    .exec(new ContainerLogReader(lc.getLogger()));

                // Wait for the container to complete, and retrieve the exit code.
                int rc = dockerClient
                    .waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitStatusCode(60, TimeUnit.SECONDS);
                LOGGER.info("Container started with exit code: {}", rc);*/

                // diagnoseContainerExit(containerId);


                // TODO: Add next steps to create the kubernetes cluster, install CNI and storage
                final Map<String, String> params = kkc.prepareTemplateParams(containerInfo);

                return 0;
            } catch (DockerClientException e) {
                LOGGER.info("Timeout to get the kind container response ...");
                return 0;
            } catch (NotModifiedException e) {
                LOGGER.info("Container is already running {}.", containerInfo.getId());
                return 0;
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

    private void waitForLogMessage(String containerId, String expectedMessage, long timeout, TimeUnit timeUnit)
        throws InterruptedException, IOException {
        final long startTime = System.currentTimeMillis();
        final long timeoutMillis = timeUnit.toMillis(timeout);
        final boolean[] messageFound = {false}; // Use an array to make it effectively final for the anonymous class
        ResultCallback<Frame> logCallback = null;
        Closeable logStream = null;

        try {
            // Use a ResultCallback to asynchronously read the logs
            logCallback = new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    String logEntry = new String(frame.getPayload());
                    if (logEntry.contains(expectedMessage)) {
                        LOGGER.info("Found expected message in logs: {}", logEntry);
                        messageFound[0] = true; // Update the flag
                        try {
                            close(); // Close the callback to stop reading logs
                        } catch (IOException e) {
                            LOGGER.warn("Error closing log callback: {}", e.getMessage());
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    LOGGER.error("Error reading container logs: {}", throwable.getMessage(), throwable);
                }
            };

            // Start listening to the logs
            LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId)
                .withFollowStream(true) // Keep the connection open to receive new logs
                .withStdOut(true)
                .withStdErr(true);

            logStream = logContainerCmd.exec(logCallback);

            // Poll for the message and check for timeout
            while (!messageFound[0] && System.currentTimeMillis() - startTime < timeoutMillis) {
                Thread.sleep(500); // Check every 500ms
            }

            if (!messageFound[0]) {
                throw new IOException("Timeout waiting for message \"" + expectedMessage + "\" in container logs for container " + containerId);
            }
        } finally {
            // Ensure resources are closed in a finally block
            if (logStream != null) {
                try {
                    logStream.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing log stream: {}", e.getMessage());
                }
            }
            if (logCallback != null) {
                try {
                    logCallback.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing log callback: {}", e.getMessage());
                }
            }
        }
    }

}
