package dev.snowdrop.command;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import dev.snowdrop.Container;
import dev.snowdrop.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

// Example Subcommand: Create
@CommandLine.Command(name = "create", description = "Create a new container")
public class CreateContainer extends Container  implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateContainer.class);
    private static final String KIND_IMAGE = "kindest/node:v1.29.14";

    @CommandLine.Parameters(index = "0", description = "The image name to use for the container")
    String containerName;

    @CommandLine.Option(names = {"-i", "--image"}, description = "The name of the image")
    String imageName;

    @CommandLine.Option(names = {"-d", "--detach"}, description = "Run the container in detached mode")
    boolean detach = false;

        /*
        @Option(names = {"--cmd"}, description = "Command to execute in the container", arity = "1..*")
        List<String> command;
        */

    @CommandLine.Option(names = {"-p", "--port"}, description = "Bind a container's port(s) to the host", arity = "1..*")
    List<String> ports;  // e.g., "8080:80", "443:443/tcp"

    @CommandLine.Option(names = {"-v", "--volume"}, description = "Bind a volume to the container", arity = "1..*")
    List<String> volumes; // e.g., "/host:/container:ro", "named-volume:/container:rw"

    @CommandLine.Option(names = {"-e", "--env"}, description = "Set environment variables", arity = "1..*")
    List<String> environment; // e.g., "VAR1=value1", "VAR2=value2"

    @Override
    public Integer call() {
        try {
            // Check if image name has been defined
            if (imageName == null) {
                imageName = KIND_IMAGE;
            }

            // Pull image
            ImageUtils.pullImage(dockerClient, imageName);

            // Build the command
            CreateContainerCmd ccc = dockerClient.createContainerCmd(imageName);

            if (containerName != null) {
                ccc.withName(containerName);
            }

            ccc.withEntrypoint("/usr/local/bin/entrypoint", "/sbin/init");
            ccc.getHostConfig().withPortBindings(PortBinding.parse("49363:6443"));

                /*
                if (ports != null && !ports.isEmpty()) {
                    List<ExposedPort> exposedPorts = new ArrayList<>();
                    List<PortBinding> portBindings = new ArrayList<>();
                    for (String portMapping : ports) {
                        String[] parts = portMapping.split(":");
                        if (parts.length > 0) {
                            String hostPort = parts[0];
                            String containerPort = parts[1];
                            String protocol = "tcp"; // Default protocol

                            if (containerPort.contains("/")) {
                                String[] portParts = containerPort.split("/");
                                containerPort = portParts[0];
                                protocol = portParts[1];
                            }

                            ExposedPort exposedPort = new ExposedPort(Integer.parseInt(containerPort), Protocol.valueOf(protocol.toUpperCase()));
                            exposedPorts.add(exposedPort);

                            if(parts.length > 1) {
                                PortBinding portBinding = new PortBinding(HostConfig.newPortBindings(hostPort),exposedPort);
                                portBindings.add(portBinding);
                            } else {
                                PortBinding portBinding = new PortBinding(HostConfig.newPortBindings(hostPort),exposedPort);
                                portBindings.add(portBinding);
                            }
                        }
                    }
                    ccc.withExposedPorts(exposedPorts.toArray(new ExposedPort[0]));
                    ccc.withPortBindings(portBindings.toArray(new PortBinding[0]));
                }
                */


            if (volumes != null && !volumes.isEmpty()) {
                List<Bind> binds = new ArrayList<>();
                for (String volumeMapping : volumes) {
                    String[] parts = volumeMapping.split(":");
                    if (parts.length > 1) {
                        String hostPath = parts[0];
                        String containerPath = parts[1];
                        String mode = "rw"; // Default mode
                        if (parts.length > 2) {
                            mode = parts[2];
                        }
                        binds.add(new Bind(hostPath, new Volume(containerPath), AccessMode.valueOf(mode.toUpperCase())));
                    }
                }
                ccc.withBinds(binds.toArray(new Bind[0]));
            }

            if (environment != null && !environment.isEmpty()) {
                ccc.withEnv(environment.toArray(new String[0]));
            }

            CreateContainerResponse containerResponse;
            try {
                 containerResponse = ccc.exec();
                String containerId = containerResponse.getId();
                LOGGER.info("Container created with ID: {}", containerId);
            } catch (InternalServerErrorException e) {
                if (e.getMessage().startsWith("Status 500: {\"cause\":\"that name is already in use\"")) {
                    LOGGER.error("Container with the same name already exist: {}", containerName);
                } else {
                    var msg = e.getMessage();
                    LOGGER.error(e.getMessage());
                }
            }

            closeDockerClient();
            return 0;

        } catch (Exception e) {
            LOGGER.error("Error creating container: {}", e.getMessage(), e);
            return 1;
        }
    }
}
